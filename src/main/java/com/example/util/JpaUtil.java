package com.example.util;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

public final class JpaUtil {
    private JpaUtil() { }
    public static EntityManagerFactory creerFactory(boolean cache, String base, boolean sql) {
        Map<String, Object> options = new HashMap<>();
        options.put("javax.persistence.jdbc.url", "jdbc:h2:mem:" + base + ";DB_CLOSE_DELAY=-1");
        options.put("hibernate.show_sql", Boolean.toString(sql));
        options.put("hibernate.cache.use_second_level_cache", Boolean.toString(cache));
        options.put("hibernate.cache.use_query_cache", Boolean.toString(cache));
        if (!cache) options.put("hibernate.cache.region.factory_class", "org.hibernate.cache.internal.NoCachingRegionFactory");
        return Persistence.createEntityManagerFactory("hibernate-cache-pu", options);
    }
}
