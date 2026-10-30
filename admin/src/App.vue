<script setup lang="ts">
import { onMounted, ref } from 'vue'
const health = ref<Record<string,string> | null>(null)
const pending = ref(false)
const error = ref('')
async function check() {
  pending.value = true
  error.value = ''
  try {
    const response = await fetch('/api/health')
    if (!response.ok) throw new Error('后端或依赖尚未就绪，请查看日志后重新检查')
    health.value = await response.json()
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '连接失败，请重新检查' }
  finally { pending.value = false }
}
onMounted(check)
</script>
<template>
  <main>
    <p class="eyebrow">FDE AI STARTER / 技术骨架</p>
    <h1>Web 管理端骨架<br>已经启动。</h1>
    <p class="lead">接下来，把你的业务需求和课程提示词交给 AI，<br>在这个工程上逐步实现。</p>
    <section aria-labelledby="connection"><h2 id="connection">服务连接</h2>
      <p v-if="pending" role="status">正在检查…</p>
      <p v-else-if="error" role="alert">{{ error }}</p>
      <dl v-else-if="health"><div><dt>Java 后端</dt><dd>{{ health.status }}</dd></div><div><dt>MySQL</dt><dd>{{ health.mysql }}</dd></div><div><dt>Redis</dt><dd>{{ health.redis }}</dd></div></dl>
      <button @click="check" :disabled="pending">{{ pending ? '检查中…' : '重新检查连接' }}</button>
    </section>
    <footer>Vue 3 + TypeScript / Java 17 / MySQL / Redis / Docker Compose</footer>
  </main>
</template>
