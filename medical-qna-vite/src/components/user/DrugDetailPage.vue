<template>
  <div>
    <h1>药品详情页面</h1>
    <p>药品ID：{{ drugId }}</p>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router';
const route = useRoute();
const drugId = route.params.drugId;
</script>
