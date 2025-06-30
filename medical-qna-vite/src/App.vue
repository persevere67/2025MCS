<template>
  <div id="app">
    <Header />
    <router-view />
    <Footer />
  </div>
</template>

<script setup>
import Header from './components/common/Header.vue';
import Footer from './components/common/Footer.vue';
</script>

<style>
#app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
</style>
