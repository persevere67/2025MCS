import { createRouter, createWebHashHistory } from 'vue-router';
import HomePage from '../components/auth/HomePage.vue';
import DrugDetailPage from '../components/user/DrugDetailPage.vue';
import QAPage from '../components/user/QAPage.vue';
import Authcontainer from '../components/auth/Authcontainer.vue';
import AdminPage from '../components/admin/AdminPage.vue';

const routes = [
  { path: '/', component: HomePage, alias: '/home' },
  { path: '/drugs/:drugId', component: DrugDetailPage },
  { path: '/qna', component: QAPage },
  { path: '/auth', component: Authcontainer },
  { path: '/admin', component: AdminPage}
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

export default router;
