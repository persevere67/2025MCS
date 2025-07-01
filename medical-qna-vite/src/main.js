import { createApp } from 'vue';
import App from './App.vue';
import router from './router/index';
import axios from 'axios';

axios.defaults.baseURL = 'http://localhost:8080'
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

createApp(App).use(router).mount('#app');
