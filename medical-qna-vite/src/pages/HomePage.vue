<template>
  <div class="home-page">
    <div class="background-slider">
      <div v-for="(image, index) in images" :key="index"
           :class="['slide', { active: index === currentImage }]">
        <img :src="image" alt="背景图" />
      </div>
    </div>

    <div class="content">
      <h1>欢迎使用神也吃拼好饭医疗问答系统</h1>
      <router-link to="/login" class="start-button">开始使用</router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: "HomePage",
  data() {
    return {
      images: [
        new URL('@/assets/bj1.jpg', import.meta.url).href,
        new URL('@/assets/bj2.jpg', import.meta.url).href,
        new URL('@/assets/bj3.jpg', import.meta.url).href,
        new URL('@/assets/bj4.jpg', import.meta.url).href,
      ],
      currentImage: 0,
      timer: null,
    };
  },
  mounted() {
    this.startSlider();
  },
  beforeUnmount() {
    clearInterval(this.timer);
  },
  methods: {
    startSlider() {
      this.timer = setInterval(() => {
        this.currentImage = (this.currentImage + 1) % this.images.length;
      }, 4000);
    },
  },
};
</script>

<style scoped>
.home-page {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.background-slider {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
}

.slide {
  opacity: 0;
  position: absolute;
  width: 100%;
  height: 100%;
  transition: opacity 1s ease-in-out;
}

.slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slide.active {
  opacity: 1;
}

.content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
  top: 40%;
  transform: translateY(-40%);
}

h1 {
  font-size: 2.5rem;
  margin-bottom: 20px;
  text-shadow: 2px 2px 6px rgba(0, 0, 0, 0.5);
}

.start-button {
  display: inline-block;
  padding: 12px 30px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border-radius: 30px;
  text-decoration: none;
  font-size: 1.2rem;
  transition: background 0.3s;
}

.start-button:hover {
  background: rgba(0, 0, 0, 0.8);
}
</style>
