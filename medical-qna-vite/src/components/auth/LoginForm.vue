<template>
  <div class="login-form">
    <h3>请登录</h3>
    <form @submit.prevent="handleSubmit">
      <div class="form-group">
        <label for="username">用户名：</label>
        <input type="text" id="username" v-model="username" placeholder="请输入用户名" />
      </div>
      <div class="form-group">
        <label for="password">密码：</label>
        <input type="password" id="password" v-model="password" placeholder="请输入密码" />
      </div>
      <div class="button-group">
        <button type="submit">登录</button>
      </div>
      <div class="register-link">
        还没有账号？ <button @click="goToRegister">去注册</button>
      </div>
    </form>
  </div>
</template>

<script>
export default {
  name: "LoginForm",
  data() {
    return {
      username: "",
      password: ""
    };
  },
  methods: {
    handleSubmit() {
      console.log("登录信息：", this.username, this.password);
      // 登录逻辑
    },
    goToRegister() {
      this.$router.push("/register"); // 假设路由路径为 /register
    }
  }
};
</script>

<style scoped>
.login-form {
  max-width: 400px;
  margin: 100px auto;
  padding: 30px;
  border: 1px solid #ddd;
  border-radius: 10px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  background: #fff;
}

h3 {
  text-align: center;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 15px;
}

label {
  display: block;
  margin-bottom: 5px;
}

input {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
}

.button-group {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

button {
  padding: 10px 20px;
  cursor: pointer;
}

.register-link {
  text-align: center;
  margin-top: 15px;
}

.register-link button {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}
</style>
