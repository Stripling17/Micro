package com.xinchen.gulimall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {
    //当前系统中池只有一两个，每个异步任务，提交给线程池。让他自己去执行
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main...start...");
//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            System.out.println("thread...start:" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("thread...end:" + i);
//        }, executor);
        /**
         * 方法完成后的感知
         */
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("thread...start:" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("thread...end:" + i);
//            return i;
//        //R apply(T t)
//        }, executor).whenComplete((res,excption) -> {
//            //虽然能得到异常信息，但是没有办法修改返回结果
//            System.out.println("Async Task Successful Complete...result="+res+";Exception is："+excption);
//        }).exceptionally(throwable -> {
//            //可以感知异常，同时返回默认值
//            return 10;
//        });
        /**
         * 方法执行完成后的处理
         */
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("thread...start:" + Thread.currentThread().getId());
//            int i = 10 / 4;
//            System.out.println("thread...end:" + i);
//            return i;
//        //R apply(T t, U u)
//        }, executor).handle((res,thr) ->{
//            if(res != null){
//                return res*2;
//            }
//            if(thr != null){
//                return 0;
//            }
//            return 0;
//        });
        /**
         * 线程串行化
         * 1）thenRun：不能获取到上一步的执行结果
         *  .thenRunAsync(() -> {
         *             System.out.println("Task Two Start");
         *         },executor);
         * 2）thenAccept：能接收上一步结果，但是无返回值
         *  .thenAcceptAsync((res) -> {
         *             System.out.println("Task Two Start" + res);
         *         });
         * 3）thenApply：既能接收上一步的结果，而且执行后有返回值
         *  .thenApplyAsync((res) -> {
         *             System.out.println("Task Two Start" + res);
         *
         *             return "Hello" + res;
         *         },executor);
         *         System.out.println("i=" + future.get());
         *
         */
//        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("thread...start:" + Thread.currentThread().getId());
//            int i = 10 / 4;
//            System.out.println("thread...end:" + i);
//            return i;
//            //R apply(T t, U u)
//            //void accept(T t);
//        }, executor).thenApplyAsync((res) -> {
//            System.out.println("Task Two Start" + res);
//            return "Hello" + res;
//        },executor);
//        System.out.println("i=" + future.get());
        /**
         * 两任务组合:都要完成
         *        future1.runAfterBothAsync(future2,() -> {
         *            System.out.println("thread_3...start:" + Thread.currentThread().getId());
         *        },executor);
         *
         *      //void accept(T t, U u);
         *        future1.thenAcceptBothAsync(future2,(t,u) -> {
         *            System.out.println("thread_3...start:" + Thread.currentThread().getId());
         *            System.out.println("Thread_1_Value=" + t + ";  " + u);
         *        },executor);
         *
         * 两任务组合，只有一个完成：
         *  1)runAfterEither:传入Runnable 无法接收完成结果的返回值【不感知结果，自己也无返回值】
         *      System.out.println("thread_2...start:" + Thread.currentThread().getId());
         *         Thread.sleep(3000);
         *         System.out.println("thread_2...end:");
         *
         *        future1.runAfterEitherAsync(future2,() -> {
         *             System.out.println("thread_3...start:" + Thread.currentThread().getId());
         *         },executor);
         *
         *  2)acceptEither:传入Consumer<? super T> action  可以接收完成任务得返回值
         *      future1.acceptEitherAsync(future2,(f) -> {
         *              System.out.println("thread_3...start:" + Thread.currentThread().getId());
         *          },executor);
         *  3)applyToEither:传入Function<? super T,U> fu可以接收完成任务得返回值，并且可以返回当前自己任务的返回值
         *          CompletableFuture<String> future = future1.applyToEitherAsync(future2,
         *                  res -> res.toString() + "->哈哈哈哈",
         *                  executor);
         */

        CompletableFuture<Object> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("thread_1...start:" + Thread.currentThread().getId());
            int i = 10 / 4;
            System.out.println("thread_1...end:" + i);
            return i;
            //R apply(T t, U u)
            //void accept(T t);
        }, executor);

        CompletableFuture<Object> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("thread_2...start:" + Thread.currentThread().getId());
            try {
                Thread.sleep(3000);
                System.out.println("thread_2...end:");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "thread_2";
        }, executor);

        //两任务组合，都要完成
//        CompletableFuture<String> future = future1.thenCombineAsync(future2, (t, u) -> {
//            System.out.println("thread_3...start:" + Thread.currentThread().getId());
//            return "Thread_1_Value=" + t + ";  " + u;
//        }, executor);
//        System.out.println("combineResult=" + future.get());

        //两任务组合:只要一个完成
        CompletableFuture<String> future = future1.applyToEitherAsync(future2, res -> res.toString() + "->哈哈哈哈", executor);

        System.out.println("applyToEitherResult=" + future.get());

        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("QueryImg");
            return "hello.jpg";
        },executor);

        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("QueryAttr");
            return "黑色256G";
        },executor);

        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("QueryDesc");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "华为";
        },executor);

        //futureImg.get();futureAttr.get();futureDesc.get();  //阻塞式等待
        //CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);
        //anyOf.get(); // allOf.join() //等待所有结果完成
        //System.out.println("main...end..."+ anyOf.get());

//        futureImg.get();futureAttr.get();futureDesc.get();  //阻塞式等待
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
        allOf.join(); //future3.get() //等待所有结果完成

        System.out.println("main...end..."+ futureImg.get()+"=>"+futureAttr.get()+"=>"+futureDesc.get());

    }

    public void thread(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main....start...");
        /**
         * 初始化线程的四种方式
         * 1）、继承 Thread
         *         Thread thread = new Thread01();
         *         thread.start(); //启动线程
         *
         * 2）、实现 Runnable 接口
         *         Runnable01 runnable01 = new Runnable01();
         *         new Thread(runnable01).start();
         *
         * 3）、实现 Callable 接口 + FutureTask （可以拿到返回结果，可以处理异常）
         *         FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
         *         new Thread(futureTask).start();
         *         //阻塞等待整个线程执行完成，获取返回结果
         *         Integer integer = futureTask.get();
         *         System.out.println("main....end..."+integer);
         *
         * 4）、线程池[ExecutorService]
         *         一般我们会将所有的多线程异步任务都交给线程池执行
         *         new Thread(() -> System.out.println("hello")).start();
         *        给线程池提交任务：
         *         submit(传入的参数 Future<?>或者Future<T> 可以带有返回值<泛型>的任务：能够获得返回值)
         *         execute(只执行任务、不返回结果：应该传入Runnable Command)
         *         service.execute(new Runnable01());
         *      4.1线程池的创建：
         *          1）Executors：使用该工具类快速创建线程池
         *          public static ExecutorService service = Executors.newFixedThreadPool(10);
         *          2）new ThreadPoolExecutor()
         *
         *      Future可以获取到异步结果
         *
         * 区别：
         *      1、2不能得到返回值。3可以获得返回值
         *      1、2、3都不能控制资源
         *      4可以控制资源，性能稳定。在高并发下可以控制线程数，防止系统崩溃
         */

        executor.execute(new Runnable01());
        /**
         * ThreadPoolExecutor [Args Constructor]
         * 七大参数:
         * corePoolSize:[5]核心线程数；线程池，创建好以后就准备就绪的线程数量，就等待来接收异步任务去执行。
         *       5个 Thread thread = new Thread();  thread(接收的异步任务).start
         *          否则核心线程数一直存在：除非设置了运行核心线程池超时【销毁】：allowCoreThreadTimeOut，
         * maximumPoolSize：[200]最大线程数量；作用是控制资源；
         * keepAliveTime：【】存活时间。如果当前的线程数量大于core
         *      释放空闲的线程（除核心线程外的线程）。只要线程空闲大于我们指定的keepAliveTime[存活时间]
         * TimeUnit unit：时间单位
         * BlockingQueue<Runnable> workQueue：阻塞队列。如果任务有很多，就会将目前多的任务放在队列里面。
         *              只要有线程空闲，就回去队列里面取出新的任务继续执行。
         * ThreadFactory：线程创建的工厂。
         * RejectedExecutionHandler handler：如果队列满啦，按照我们指定的拒绝策略拒绝执行任务
         *
         *
         *  工作顺序：
         *  1）线程池创建，准备好 core 数量的核心线程，准备接受任务
         *      1、core 满了，就将再进来的任务放入阻塞队列中。空闲的 core 就会自己去阻塞队列获取任务执行
         *      2、阻塞队列满了，就直接开新线程执行，最大只能开到 max 指定的数量
         *      3、maximum满啦就用RejectedExecutionHandler：拒绝策略来拒绝任务
         *      4.max都执行完成，有很多空闲，在指定时间keepAliveTime以后，根据设定的超时时间
         *          释放除核心线程外的全部线程 max-core
         *
         *      new LinkedBlockingDeque<></>():默认是Integer的最大值：导致内存不够
         *
         * 一个线程池 core 7； max 20 ，queue：50，100 并发进来怎么分配的；
         *      7个线程会立即得到执行，50个会进入队列，再开13个线程进行执行。剩下的30个就是用拒绝策略。
         *    如果不想抛弃还要执行CallerRunPolicy;-->直接执行Runnable方法
         */
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        //带缓存的线程池：没有核心数量，可以创建Integer最大值的线程，存活时间60s ->所有线程都可以回收
        Executors.newCachedThreadPool();
        //固定值大小的线程池：指定10-> 核心是10；最大是10
        //Executors.newFixedThreadPool(); //core=max 所有线程都不可回收

        //定时任务的线程池
        //Executors.newScheduledThreadPool();

        //单线程的线程池:后台丛队列里面获取任务，挨个执行
        //Executors.newSingleThreadExecutor();

        System.out.println("main....end...");
    }

    public static class Thread01 extends Thread {
        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果" + i);
        }
    }

    public static class Runnable01 implements Runnable {

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果" + i);
        }
    }

    public static class Callable01 implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("运行结果" + i);
            return i;
        }
    }
}

