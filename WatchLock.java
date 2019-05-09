package com.security.demo.redis;

import org.apache.commons.collections4.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * redis使用watch指令实现分布式乐观锁,demo
 * 思路:
 * 业务场景-->
 *     redis存储了用户的账户余额,现在有两个客户端并发请求要对账户余额进行修改,我们一般都需要取出余额做修改,之后再写回redis,
 *     那么这里就有一个并发问题,我们可以用redis的分布式锁来解决问题.我们还可以用redis提供的watch机制来解决并发修改的问题,它其实是一个乐观锁.
 *
 *     我们需要在multi命令开启之前去watch某个变量,当执行到exec指令的时候,redis会检查这个变量是否有变化,如果有变化,
 *     那么redis认为当前变量被修改过或者正在被修改,它会给客户端返回一个null,,客户端此时需要重试,直到返回值不为null,
 *     一般此操作可以在while(true)中进行,当返回值不为null时,break即可
 *
 *
 *
 * 这样子有问题吗大佬?求指正!
 *
 */
public class WatchLock {
    public static void main(String[] args) {
        Jedis jedis = new Jedis();
        String userId = String.format("account_%s", "abc");
        jedis.setnx(userId, String.valueOf(5));
        int value = 0;
        while (true){
            jedis.watch(userId);
            value = Integer.parseInt(jedis.get(userId));
            value *= 2;
            Transaction transaction = jedis.multi();
            transaction.set(userId, String.valueOf(value));
            List<Object> list = transaction.exec();
            //如果返回值为空,说明有其它客户端在操作,当前操作需要稍后重试
            if(CollectionUtils.isNotEmpty(list)){
                break;
            }
        }
        System.out.println(value);
        jedis.close();
    }
}
