import { createRouter,createWebHistory } from 'vue-router';
import HomePage from '../components/HomePage.vue';
import DrugDetailPage from '../components/user/DrugDetailPage.vue';
import QAPage from '../components/user/QAPage.vue';
import AuthContainer from '../components/auth/AuthContainer.vue';
import AdminPage from '../components/admin/AdminPage.vue';
import { authUtils } from '@/utils/api';
import UserHistoryPage from '../components/admin/UserHistoryPage.vue';

const routes = [
  { 
    path: '/', 
    name: 'Home',
    component: HomePage, 
    alias: '/home',
    meta: { 
      requiresAuth: false,
      title: '医药问答系统 - 首页'
    }
  },
  { 
    path: '/qna', 
    name: 'QNA',
    component: QAPage,
    meta: { 
      requiresAuth: true, 
      role: 'USER',
      title: 'AI问答 - 医药问答系统'
    }
  },
  { 
    path: '/auth', 
    name: 'Auth',
    component: AuthContainer,
    meta: { 
      requiresAuth: false,
      title: '用户登录 - 医药问答系统'
    }
  },
  { 
    path: '/admin', 
    name: 'Admin',
    component: AdminPage,
    meta: { 
      requiresAuth: true, 
      role: 'ADMIN',
      title: '管理后台 - 医药问答系统'
    }
  },

  { 
  path: '/user-history/:userId',  // 添加:userId占位符
  name: 'UserHistory',
  component: UserHistoryPage,
  meta: { 
    requiresAuth: true, 
    role: 'ADMIN',
    title: '用户历史记录 - 医药问答系统'
  }
}
];

const router = createRouter({
  history: createWebHistory(),
  

  
  routes,
  
  // 路由切换时滚动到顶部
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      return { top: 0 };
    }
  }
});

router.beforeEach(async (to, from, next) => {
  console.log('🔒 路由守卫检查:', {
    to: to.path,
    from: from.path,
    requiresAuth: to.meta.requiresAuth,
    role: to.meta.role
  });

  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title;
  }

  const isLoggedIn = authUtils.isLoggedIn();
  const userRole = authUtils.getUserRole();

  // 权限检查
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isLoggedIn) {
      console.log('❌ 用户未登录，跳转到认证页面');
      next({ path: '/auth', query: { redirect: to.fullPath } }); // 记录原路径
    } else if (to.meta.role && userRole !== to.meta.role) {
      console.log('❌ 用户角色不符，当前角色:', userRole, '需要角色:', to.meta.role);
      
      // 根据用户角色重定向
      if (userRole === 'ADMIN') {
        next(); // 管理员可以访问所有页面
      } else if (userRole === 'USER') {
        next('/qna'); // 普通用户跳转到问答页面
      } else {
        next('/'); // 其他情况跳转到首页
      }
    } else {
      next();
    }
  } else {
    next();
  }
});

// 全局后置钩子
router.afterEach((to, from) => {
  console.log('✅ 路由切换完成:', {
    to: to.path,
    from: from.path
  });
});

export default router;