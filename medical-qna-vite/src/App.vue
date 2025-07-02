<template>
  <div id="app">
    <Header />
    <router-view />
    <Footer />
  </div>
</template>

<script>
import Header from './components/common/Header.vue';
import Footer from './components/common/Footer.vue';

export default {
  components: { Header, Footer },
};
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
}
</style>
