package com.xinchen.gulimall.product.web;

import com.xinchen.gulimall.product.entity.CategoryEntity;
import com.xinchen.gulimall.product.service.CategoryService;
import com.xinchen.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        //TODO 1、查出所有的1级分类
        List<CategoryEntity> categoryEntityList =  categoryService.getLevel1Categorys();

        //默认前缀：classpath:/templates
        //默认后缀：.html
        model.addAttribute("categorys",categoryEntityList);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/json/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson(){
        Map<String, List<Catelog2Vo>> catelogJson = categoryService.getCatelogJson();

        return catelogJson;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        //1.获取一把锁，只要锁的名字一样，就是同一把锁
        RLock lock = redisson.getLock("my-lock");


        //2.加锁
        //lock.lock(); //阻塞式等待
        //redis分布式锁特性：锁的自动续期，如果业务超长，运行期间自动给锁续上新的30s。不动担心业务时间过长，锁自动过期被删掉
        //加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，即使不手动解锁，锁默认在30s后删除

        //加锁：指定时间：
        lock.lock(10, TimeUnit.SECONDS);//10秒以后自动解锁，自动解锁时间，一定要大于业务的执行时间
        //问题：lock.lock(10, TimeUnit.SECONDS);在超时后不会自动续期
        //1.如果我们传递了锁的超时时间，就发送给redis执行脚本，进行占锁，默认超时就是我们指定的时间
        //2.如果我们未指定锁的超时时间，就是用30*1000【LookWatchdogTimeout：看门狗默认时间】
        //  只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门狗默认时间】
        //  定时任务在什么时候开始续期 internalLockLeaseTime【看门狗时间】/3，10s以后续期

        //最佳实战
        //1）、 lock.lock(10, TimeUnit.SECONDS);省掉了续期操作。手动解锁
        try {
            System.out.println("加锁成功，执行业务..." + Thread.currentThread().getId());
            Thread.sleep(30000);
        }catch(Exception e) {

        }finally {
            //解锁
            System.out.println("释放锁" + Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }

    //读写锁的好处：保证一定能够读到最新数据，修改期间，写锁是一个排他锁【互斥锁】【独享锁】
    // （并发期间，只能存在一个写锁【阻塞】） 读锁是一个共享锁，但是如果写锁没有释放，读锁必须等待
    @ResponseBody
    @GetMapping("/write")
    public String writeValue() {

        String s = "";
        //ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        RLock wLock = lock.writeLock();
        try {
            //1.改数据加写锁，读数据加读锁
            wLock.lock();
            System.out.println("写锁加锁成功" + Thread.currentThread().getId());
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            stringRedisTemplate.opsForValue().set("writeValue",s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            wLock.unlock();
            System.out.println("写锁释放" + Thread.currentThread().getId());
        }

        return s;
    }

    /**
     * 读 + 读：相当于无锁，并发读；
     * 写 + 并发度；在写的过程中，只会在redis中记录好所有的读锁，待写锁释放后他们会同时加锁成功
     * 写 + 写：阻塞方式
     * 读 + 写：有读锁，写也需要等待
     * ※ 只要有写的存在，都必须等待
     * @return
     */
    @ResponseBody
    @GetMapping("/read")
    public String readValue() {

        String s = "";

        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        RLock rLock = lock.readLock();
        try {
            rLock.lock();
            System.out.println("读锁加锁成功" + Thread.currentThread().getId());
            s = stringRedisTemplate.opsForValue().get("writeValue");
            Thread.sleep(30000);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
            System.out.println("读锁释放" + Thread.currentThread().getId());
        }

        return s;
    }

    /**
     * 分布式信号量：未来可以做分布式的限流操作
     * @return
     * @throws InterruptedException
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
        //park.acquire(); //获取一个信号，获取一个值，占一个车位
        //尝试获取一下执行，不行就算啦
        boolean b = park.tryAcquire();
        if(b){
            //执行业务
        }else {
            return "error:'当前流量过大，请稍等'";
        }

        return "ok=>"+b;

    }

    @GetMapping("/go")
    @ResponseBody
    public String go() {
        RSemaphore park = redisson.getSemaphore("park");
        park.release(); //释放一个车位

//        Semaphore semaphore = new Semaphore(5);
//        semaphore.release();
//
//        semaphore.release();

        return "ok";

    }

    /**
     * 放假，锁门
     * 1班没人了，2
     * 5个班全部走完，我们可以锁大门
     */
    //分布式的闭锁
    @ResponseBody
    @GetMapping("/lockDoor")
    public String lockDoor() {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        try {
            door.trySetCount(5);
            //等待闭锁都完成
            door.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "放假啦...";
    }

    @ResponseBody
    @GetMapping("/gogogo/{id}")
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();  //计数减一

        return id+"班的人都走啦...";
    }
}
/**
     private Optional<Resource> getWelcomePage() {
     String[] locations = WebMvcAutoConfiguration.getResourceLocations(this.resourceProperties.getStaticLocations());
     return Arrays.stream(locations).map(this::getIndexHtml).filter(this::isReadable).findFirst();
     }

 private static final String[] CLASSPATH_RESOURCE_LOCATIONS =
 new String[]{"classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/public/"};
 */

//解决办法：
//应该以什么样的路径来访问静态资源,这表示只有静态资源的访问路径为/static/ 时才会处理(如http://localhost:8080/static/css/base.css)
//spring.mvc.static-path-pattern: /static/**
