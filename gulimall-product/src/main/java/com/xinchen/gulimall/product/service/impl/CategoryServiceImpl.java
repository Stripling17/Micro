package com.xinchen.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xinchen.gulimall.product.service.CategoryBrandRelationService;
import com.xinchen.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinchen.common.utils.PageUtils;
import com.xinchen.common.utils.Query;

import com.xinchen.gulimall.product.dao.CategoryDao;
import com.xinchen.gulimall.product.entity.CategoryEntity;
import com.xinchen.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    //本地缓存
    //private Map<String,Object> cache = new HashMap<>();

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redisson;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查出所有分类以及子分类，以树形结构组装起来
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2.组装成父子的树形结构
        //2.1)找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                        categoryEntity.getParentCid() == 0
                //2.2)使用map映射方法
        ).map(menu -> {
            //将当前菜单的子分类保存进去
            menu.setChildren(getChildrens(menu, entities));
            return menu;
            //2.3)找到父菜单后，使用sorted API排序父菜单
        }).sorted((menu1, menu2) ->
                //前面的菜单==>menu1，后面的菜单==>menu2
                (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort())
        ).collect(Collectors.toList());

        return level1Menus;
    }

    /**
     * 批量删除商品分类信息
     *
     * @param asList
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单，是否被其他地方引用
        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);
        return (Long[]) parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * 缓存失效模式
     * @CacheEvict 菜单一旦被修改后，就在更新后删除已存在的缓存。为下一次查询重新获得更新后的缓存做准备
     * @Caching(evict = {})：同时进行多种缓存操作
     * @CacheEvict(value = "category" , allEntries = true) :指定删除某个分区下的所有数据
     * 存储同一类型的数据（同一个表【同一个对象】）, 都可以指定成同一个分区。分区名默认就是缓存的前缀
     */
    //@CacheEvict(value = "category", key = "'getLevel1Categorys'")
    //
//    @Caching(evict = {
//            @CacheEvict(value = "category", key = "'getLevel1Categorys'"),
//            @CacheEvict(value = "category", key = "'getCatelogJson'")
//    })
    @CacheEvict(value = "category" , allEntries = true) //失效模式
    //@CachePut：双写模式：清楚缓存后，拿到新的返回值在往redis里面存一份
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

        //同时修改缓存中的数据
        //redis.del("catalogJson);等待下次主动查询进行更新
    }

    /**
     * 1.每一个需要缓存的数据我们都来指定要放到哪个名字的缓存。【缓存的分区（按照业务类型分）】
     * 2.默认行为：1）如果缓存中有，方法不调用。
     * 2）key是默认自动生成的：缓存的名字::SimpleKey [](自主生成的键)
     * 3）缓存value的值。默认使用jdk序列化机制。将序列化后的数据存到redis
     * 4）默认时间（TTL）是-1，代表永不过期
     * 3.自定义操作（例如我们希望所有的缓存都有过期时间）
     * 1）指定生成的缓存使用的key     使用key属性指定，接收一个SpEL表达式-->文档：https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache-spel-context
     * 2）指定缓存的数据的存活时间     配置文件中修改：spring.cache.redis.time-to-live=360000
     * 3）将数据保存为JSON格式
     * 自定义缓存管理器：
     * 学习如何修改默认缓存管理器的一些配置（需要理清缓存管理器之类的关系）
     * 1）CacheAutoConfiguration帮我们导入了RedisCacheConfiguration
     * 2）RedisCacheConfiguration中的RedisCacheManager会帮我们把我们配置文件中所配置的缓存名字初始化所有的缓存
     * 3）通过DetermineConfiguration()决定我们缓存使用哪一个配置：通过拿到RedisCacheConfiguration缓存配置来决定
     */
    @Cacheable(value = {"category"}, key = "#root.method.name" , sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("getLevel1Categorys");
//        long l = System.currentTimeMillis();
        List<CategoryEntity> categoryEntityList = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
//        System.out.println("消耗时间："+(System.currentTimeMillis()-l));
        return categoryEntityList;
    }

    @Cacheable(value = {"category"}, key = "#root.method.name")
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        System.out.println("查询了数据库......");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1.查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        //2.封装数据（一级菜单的ID为MAP的key）
        Map<String, List<Catelog2Vo>> listWithJSON = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1)每一个的一级分类，查到这个一级分类的所有二级分类
            List<CategoryEntity> level2Categorys = getParent_cid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (level2Categorys != null) {
                //2)封装数据（封装二级分类的普通变量 与 引用【三级分类信息】）
                catelog2Vos = level2Categorys.stream().map(item -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, item.getCatId().toString(), item.getName());
                    //1))每一个的二级分类，查到这个二级分类的所有三级分类
                    List<CategoryEntity> level3Categorys = getParent_cid(selectList, item.getCatId());
                    if (level3Categorys != null) {
                        //2))封装数据（封装三级分类信息）
                        List<Catelog2Vo.Catelog3Vo> catelog3Vos = level3Categorys.stream().map(i -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(item.getCatId().toString(), i.getCatId().toString(), i.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        return listWithJSON;
    }

    //TODO 产生堆外内存溢出：OutOfDirectMemoryError
    //1）、springboot2.0以后默认使用lettuce作为操作redis的客户端，它使用进行网络通信
    //2）、lettuce的bug导致netty堆外内存溢出 -Xmx300M;netty如果没有指定堆外内存，默认使用-Xmx300M
    //    可以通过 -Dio.netty.maxDirectMemory: {} bytes ==》来指定堆外内存
    //解决方法；不能使用-Dio.netty.maxDirectMemory，只去调大堆外内存
    //1）、升级lettuce客户端    2）、切换使用jedis
    // redisTemplate:
    // lettuce、jedis操作redis的最底层的客户端，然后spring对它们再次封装，封装为redisTemplate

    //    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson2() {
        /**
         * 1.空结果缓存：解决缓存的穿透问题；
         * 2.设置过期时间（加随机值）：解决缓存雪崩
         * 3.加锁：解决缓存击穿问题
         */


        //1.加入缓存逻辑（缓存中所有存储的数据都是json字符串） ==》json跨语言，跨平台兼容
        String catelogJson = redisTemplate.opsForValue().get("catelogJson");
        if (!StringUtils.hasText(catelogJson)) {
            //2.缓存中没有数据（缓存为空）
            //查询数据库
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedissonLock();
            return catelogJsonFromDb;
        }

        //转为我们指定的对象
        Map<String, List<Catelog2Vo>> reslut = JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return reslut;
    }

    //使用redisson的分布式锁来访问缓存及数据库
    //缓存数据一致性：缓存里的数据如何和数据库保持一致
    //1) 双写模式 2) 失效模式
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedissonLock() {
        //1.锁的名字：锁的粒度,越细越快
        //锁的粒度：具体缓存的是某个数据，11-号商品；   product-11-lock,product-12-lock
        RLock lock = redisson.getLock("catelogJson-lock");
        lock.lock();

        Map<String, List<Catelog2Vo>> catalogJsonFromDb;
        try {
            //加锁成功...   执行业务
            catalogJsonFromDb = getCatalogJsonFromDb();
        } finally {
            lock.unlock();
        }
        return catalogJsonFromDb;
    }

    //使用redis的分布式锁来访问缓存及数据库
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock() {
        //1.占分布式锁。去redis占坑 ：向redis保存一个键相同的东西
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock == true) {
            System.out.println("获取分布式锁成功...");
            //2.设置过期时间,必须和加锁是同步的,原子的
            //redisTemplate.expire("lock",30,TimeUnit.SECONDS)
            //锁的自动续期
            Map<String, List<Catelog2Vo>> catalogJsonFromDb;
            try {
                //加锁成功...   执行业务
                catalogJsonFromDb = getCatalogJsonFromDb();
            } finally {
                //Lua脚本解锁（原子性）
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<Integer>(script, Integer.class)
                        , Arrays.asList("lock"), uuid);
            }
            //获取值后对比+对比成功后删除=原子操作
            /*
            String lockValue = redisTemplate.opsForValue().get("lock");
            //删除锁前确认删除的不是别人的锁
            if(uuid.equals(lockValue)){
                //删除我自己的锁
                redisTemplate.delete("lock"); //删除锁
            }
            */
            return catalogJsonFromDb;
        } else {
            //休眠100ms重试
            //加锁失败...重试。 类似于本地同步锁synchronized会一直监听，只要一释放（自旋）：一直重试重试
            System.out.println("获取分布式锁失败...等待重试");
            return getCatelogJsonFromDbWithRedisLock();
        }
    }


    //从数据库查询封装分类数据
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithLocalLock() {
        //1.如果缓存中有就用缓存的
        //Map<String, List<Catelog2Vo>> catelogJson = (Map<String, List<Catelog2Vo>>) cache.get("getCatelogJson");
        //if(cache.get("getCatelogJson") == null){ }

        //只要是同一把锁，就能锁住需要这个锁的所有进程    同步代码块、方法上加
        //1.synchronized (this){} ==>SpringBoot所有的组件在容器中都是单例的
        //TODO 本地锁：synchronized,JUC(Lock)   分布式锁：在分布式情况下使用
        synchronized (this) {
            //得到锁以后，我们应该在去缓存中确定一次，如果没有才需要继续查询
            return getCatalogJsonFromDb();
        }
    }

    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {
        //得到锁以后，我们应该在去缓存中确定一次，如果没有才需要继续查询
        String catelogJson = redisTemplate.opsForValue().get("catelogJson");
        if (StringUtils.hasText(catelogJson)) {
            //缓存不为空直接返回（转为我们指定的对象）
            Map<String, List<Catelog2Vo>> reslut = JSON.parseObject(catelogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return reslut;
        }
        System.out.println("查询了数据库......");
        /**
         * 将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //1.查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //2.封装数据（一级菜单的ID为MAP的key）
        Map<String, List<Catelog2Vo>> listWithJSON = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1)每一个的一级分类，查到这个一级分类的所有二级分类
            List<CategoryEntity> level2Categorys = getParent_cid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (level2Categorys != null) {
                //2)封装数据（封装二级分类的普通变量 与 引用【三级分类信息】）
                catelog2Vos = level2Categorys.stream().map(item -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, item.getCatId().toString(), item.getName());
                    //1))每一个的二级分类，查到这个二级分类的所有三级分类
                    List<CategoryEntity> level3Categorys = getParent_cid(selectList, item.getCatId());
                    if (level3Categorys != null) {
                        //2))封装数据（封装三级分类信息）
                        List<Catelog2Vo.Catelog3Vo> catelog3Vos = level3Categorys.stream().map(i -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(item.getCatId().toString(), i.getCatId().toString(), i.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catelog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        //将对象转为json，放入缓存中
        String jsonString = JSON.toJSONString(listWithJSON);
        redisTemplate.opsForValue().set("catelogJson", jsonString, 1, TimeUnit.DAYS);
        return listWithJSON;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream()
                .filter(item -> item.getParentCid() == parent_cid)
                .collect(Collectors.toList());
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
        return collect;
    }

    //[225,25,2]
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    //通过递归的方法，找到所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == root.getCatId()
        ).map(categoryEntity -> {
            //1.找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) ->
                //2.菜单的排序
                (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort())
        ).collect(Collectors.toList());

        return children;
    }

}
