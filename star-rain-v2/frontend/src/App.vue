<script setup>
import { ref } from 'vue'

const message = ref('你好，星雨笔录 V2')
const result = ref('')
const error = ref('')
const sending = ref(false)

async function sendEcho() {
  result.value = ''
  error.value = ''
  sending.value = true

  try {
    const response = await fetch('/api/v2/echo', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: message.value }),
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const data = await response.json()
    result.value = `后端 V${data.version} 回传：${data.message}`
  } catch (cause) {
    error.value = `请求失败：${cause instanceof Error ? cause.message : String(cause)}`
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <main class="panel">
    <p class="eyebrow">STAR RAIN NOTES · V2.0</p>
    <h1>前后端 HTTP 联调</h1>
    <p class="description">输入一段文字，发送到 V2 后端，再查看回传结果。</p>

    <form @submit.prevent="sendEcho">
      <label for="echo-message">发送内容</label>
      <input id="echo-message" v-model="message" type="text" />
      <button type="submit" :disabled="sending">
        {{ sending ? '发送中…' : '发送 HTTP 请求' }}
      </button>
    </form>

    <p v-if="result" class="result" role="status">{{ result }}</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
  </main>
</template>

