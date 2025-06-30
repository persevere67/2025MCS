<!-- src/components/auth/RegisterForm.vue -->
<template>
  <form class="register-form">
    <h3>请输入您的信息</h3>
    <div class="form-group">
      <label for="username">用户名：</label>
      <input type="text" id="username" v-model="username" placeholder="请输入用户名" />
    </div>
    <div class="form-group">
      <label for="email">邮箱：</label>
      <input type="email" id="email" v-model="email" placeholder="请输入邮箱" />
    </div>
    <div class="form-group">
      <label for="password">密码：</label>
      <input type="password" id="password" v-model="password" placeholder="请输入密码" />
    </div>
    <button type="submit">注册</button>
  </form>
</template>

<script>
export default {
  name: 'RegisterForm',
  data() {
    return {
      username: '',
      email: '',
      password: '',
    };
  },
};
</script>

<style scoped>
.register-form {
  text-align: center;
}

.form-group {
  margin-bottom: 10px;
}
</style>
