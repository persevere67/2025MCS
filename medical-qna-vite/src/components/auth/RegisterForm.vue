<template>
  <div class="register-form">
    <h3>用户注册</h3>
    <form @submit.prevent="handleRegister">
      <div class="form-group">
        <label for="username">用户名：</label>
        <input type="text" id="username" v-model="username" placeholder="请输入用户名" />
      </div>
      <div class="form-group">
        <label for="password">密码：</label>
        <input type="password" id="password" v-model="password" placeholder="请输入密码" />
      </div>
      <div class="form-group">
        <label for="confirmPassword">确认密码：</label>
        <input type="password" id="confirmPassword" v-model="confirmPassword" placeholder="请再次输入密码" />
      </div>
      <div class="button-group">
        <button type="submit">注册</button>
      </div>
      <div class="login-link">
        已有账号？ <button @click="goToLogin">去登录</button>
      </div>
    </form>
  </div>
</template>

<script>
import { ref } from 'vue';
import axios from 'axios';

const username = ref("");
const password = ref("");
const confirmPassword = ref("");

const handleRegister = async() => {
  try {
    await axios.post("/api/register", {
      username: username.value,
      password: password.value
    });
    alert("注册成功！");
    $emit('switchForm','login')
    } catch (error) {
      alert("注册失败！");
    }
};
  

export default {
  name: "RegisterForm",
  data() {
    return {
      username: "",
      password: "",
      confirmPassword: ""
    };
  },
  methods: {
    handleRegister() {
      if (this.password !== this.confirmPassword) {
        alert("两次输入的密码不一致！");
        return;
      }
      console.log("注册信息：", this.username, this.password);
      // 注册逻辑
    },
    goToLogin() {
      this.$router.push("/login");
    }
  }
};
</script>

<style scoped>
.register-form {
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

.login-link {
  text-align: center;
  margin-top: 15px;
}

.login-link button {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}
</style>
