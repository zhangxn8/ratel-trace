package com.ratel.domain;

import org.elasticsearch.search.sort.SortOrder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhangxn
 * @description: 排序对象封装
 * @date 2021/12/9  22:08
 */
public class Sort {
    private List<Order> orders = null;


    public List<Order> listOrders(){
        return orders;
    }


    public Sort(Sort.Order... ods) {
        orders = new ArrayList<>();
        for (Order od : ods) {
            orders.add(od);
        }
    }

    public Sort and(Sort sort) {
        if(orders == null){
            orders = new ArrayList<>();
        }
        sort.orders.forEach(order -> orders.add(order));
        return this;
    }

    public static class Order implements Serializable {
        private final SortOrder direction;
        private final String property;

        public Order(SortOrder direction,String property){
            this.direction = direction;
            this.property = property;
        }

        public SortOrder getDirection() {
            return direction;
        }

        public String getProperty() {
            return property;
        }
    }
}
