package com.medical.qna.medical_qna_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.lang.NonNull;
import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        
        // 🔧 优先处理具体的静态资源
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(31536000); // 1年缓存

        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico")
                .setCachePeriod(31536000);

        registry.addResourceHandler("/vite.svg")
                .addResourceLocations("classpath:/static/vite.svg")
                .setCachePeriod(31536000);

        // 🎯 改进的SPA路由处理
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0) // HTML不缓存
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
                        
                        // 🚫 API请求直接跳过，不处理
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        
                        // 📁 尝试查找实际的静态资源
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // ✅ 如果资源存在且可读，直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // 🎯 对于不存在的资源，检查是否是Vue路由路径
                        if (isVueRoute(resourcePath)) {
                            // 返回index.html让Vue Router处理
                            Resource indexHtml = location.createRelative("index.html");
                            if (indexHtml.exists() && indexHtml.isReadable()) {
                                return indexHtml;
                            }
                        }
                        
                        // 其他情况返回null，让Spring处理
                        return null;
                    }
                    
                    /**
                     * 判断是否是Vue路由路径
                     */
                    private boolean isVueRoute(String resourcePath) {
                        // Vue路由路径列表
                        String[] vueRoutes = {
                            "qna", "admin", "auth", "home",
                            "qna/", "admin/", "auth/", "home/"
                        };
                        
                        // 检查是否匹配Vue路由
                        for (String route : vueRoutes) {
                            if (resourcePath.equals(route) || resourcePath.startsWith(route + "/")) {
                                return true;
                            }
                        }
                        
                        return false;
                    }
                });
    }
}