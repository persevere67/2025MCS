  <!-- src/components/auth/LoginForm.vue -->
  <template>
    <div class="login-form">
      <h3>登录表单</h3>
      <form>
        <div>
          <label for="username">用户名：</label>
          <input type="text" id="username" v-model="username" placeholder="请输入用户名" />
        </div>
        <div>
          <label for="password">密码：</label>
          <input type="password" id="password" v-model="password" placeholder="请输入密码" />
        </div>
        <button type="submit">登录</button>
      </form>
    </div>
  </template>

  <script>
  export default {
    name: 'LoginForm',
    data() {
      return {
        username: '',
        password: '',
      };
    },
  };
  </script>

  <style scoped>
  .login-form {
    text-align: center;
    padding: 20px;
  }

  form {
    display: flex;
    flex-direction: column;
  }

  label {
    margin-right: 10px;
  }

  input {
    padding: 5px;
    margin-bottom: 10px;
  }
  </style>
  