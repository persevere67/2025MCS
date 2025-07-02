<template>
  <footer class="footer">
    <div class="footer-content">
      <span>© 神也吃拼好饭团队 2025</span>
    </div>
  </footer>
</template>

<script>
export default {
  name: "Footer",
};
</script>

<style scoped>
.footer {
  position: fixed;
  bottom: 0;
  width: 100%;
  background: linear-gradient(90deg, #86cbbb, #0bbaba);
  color: #fff;
  text-align: center;
  padding: 10px 0;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.3);
  z-index: 1000;
}

.footer-content {
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

@media (max-width: 600px) {
  .footer-content {
    font-size: 13px;
  }
}
</style>
