import { createApp } from 'vue';
import App from './App.vue';
import router from './router/index';
import { createPinia } from 'pinia';
import { useAuthStore } from './stores/auth';
import axios from 'axios';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.mount('#app');

const authStore = useAuthStore();
authStore.initialize();

axios.defaults.baseURL = 'http://localhost:8080'
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

createApp(App).use(router).mount('#app');
