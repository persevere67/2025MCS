import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '../pages/HomePage.vue';
import LoginPage from '../pages/LoginPage.vue';
import RegisterPage from '../pages/RegisterPage.vue';
import HistoryPage from '../pages/HistoryPage.vue';
import DrugDetailPage from '../pages/DrugDetailPage.vue';
import QAPage from '../pages/QAPage.vue';

const routes = [
  { path: '/', component: HomePage, alias: '/home' },
  { path: '/login', component: LoginPage },
  { path: '/register', component: RegisterPage },
  { path: '/history', component: HistoryPage },
  { path: '/drugs/:drugId', component: DrugDetailPage },
  { path: '/qna', component: QAPage },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
