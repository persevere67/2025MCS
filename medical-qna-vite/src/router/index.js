import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '../components/HomePage.vue';
import DrugDetailPage from '../components/user/DrugDetailPage.vue';
import QAPage from '../components/user/QAPage.vue';
import Authcontainer from '../components/auth/Authcontainer.vue';
import AdminPage from '../components/admin/AdminPage.vue';
import { authUtils } from '@/utils/api';

const routes = [
  { path: '/', component: HomePage, alias: '/home' },
  { path: '/drugs/:drugId', component: DrugDetailPage },
  { 
    path: '/qna', 
    component: QAPage,
    meta: { requiresAuth: true } // 添加认证要求
  },
  { path: '/auth', component: Authcontainer },
  { 
    path: '/admin', 
    component: AdminPage,
    meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 添加全局前置守卫
router.beforeEach((to, from, next) => {
  console.log('路由导航:', from.path, '->', to.path);
  
  // 检查是否需要认证
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!authUtils.isLoggedIn()) {
      console.log('需要认证但用户未登录，跳转到首页');
      next('/');
    } else {
      console.log('用户已登录，允许访问');
      next();
    }
  } else {
    console.log('无需认证，直接访问');
    next();
  }
});

// 添加全局后置钩子，用于调试
router.afterEach((to, from) => {
  console.log('路由跳转完成:', from.path, '->', to.path);
});

export default router;