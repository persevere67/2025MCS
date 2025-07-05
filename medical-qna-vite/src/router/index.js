import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '../components/HomePage.vue';
import DrugDetailPage from '../components/user/DrugDetailPage.vue';
import QAPage from '../components/user/QAPage.vue';
import AuthContainer from '../components/auth/AuthContainer.vue';
import AdminPage from '../components/admin/AdminPage.vue';
import { authUtils } from '@/utils/api';

const routes = [
  { path: '/', component: HomePage, alias: '/home' },
  { path: '/drugs/:drugId', component: DrugDetailPage },
  { 
    path: '/qna', 
    component: QAPage,
    meta: { requiresAuth: true, role: 'USER' } // 角色要求
  },
  { path: '/auth', component: AuthContainer },
  { 
    path: '/admin', 
    component: AdminPage,
    meta: { requiresAuth: true, role: 'ADMIN' } // 区分角色
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to, from, next) => {
  const isLoggedIn = authUtils.isLoggedIn();
  const userRole = authUtils.getUserRole(); // 实现角色获取逻辑

  // 权限检查
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isLoggedIn) {
      next({ path: '/auth', query: { redirect: to.fullPath } }); // 记录原路径
    } else if (to.meta.role && userRole !== to.meta.role) {
      next('/403'); // 角色不符
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;