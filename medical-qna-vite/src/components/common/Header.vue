<template>
  <header class="header">
    <nav class="nav-bar">
      <div class="nav-left">
        <router-link to="/" class="nav-link">神也吃拼好饭医疗问答系统</router-link>
      </div>
      <div class="nav-right">
        <router-link to="/login" class="nav-link">登录</router-link>
        <router-link to="/register" class="nav-link">注册</router-link>
      </div>
    </nav>
  </header>
</template>

<script>
export default {
  name: "Header",
};
</script>

<style scoped>
.header {
  background: linear-gradient(90deg, #86cbbb, #0bbaba);
  padding: 12px 36px;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-link {
  color: #fff;
  margin-left: 20px;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s;
}

.nav-link:hover {
  color: #71ece8;
}

.nav-left .nav-link {
  font-size: 18px;
  font-weight: bold;
}
</style>
