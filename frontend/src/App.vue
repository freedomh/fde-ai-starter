<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { request, type Student, type Feedback } from './api'
const students=ref<Student[]>([])
const selected=ref<number|null>(null)
const items=ref<Feedback[]>([])
const loading=ref(true)
const error=ref('')
async function loadFeedback(){
  loading.value=true;error.value=''
  try{items.value=selected.value?await request<Feedback[]>('/students/'+selected.value+'/feedback'):[]}
  catch(cause){error.value=cause instanceof Error?cause.message:'读取失败，请重试'}finally{loading.value=false}
}
onMounted(async()=>{
  try{students.value=await request<Student[]>('/students');selected.value=students.value[0]?.id??null;await loadFeedback()}
  catch(cause){error.value=cause instanceof Error?cause.message:'读取失败';loading.value=false}
})
</script>
<template>
  <main class="student-layout">
    <header><div class="brand">课程反馈台</div><span>学员端 · 本地演示 / 虚构数据</span></header>
    <section class="intro"><h1>看看这节课的收获。</h1><p>这里展示教师已经确认的反馈，方便回顾表现和课后任务。</p></section>
    <section class="student-toolbar" aria-label="选择演示学员"><div><label for="selected-student">演示学员</label><select id="selected-student" v-model="selected" @change="loadFeedback" :disabled="loading"><option v-for="student in students" :key="student.id" :value="student.id">{{ student.name }}</option></select></div><button class="secondary" :disabled="loading" @click="loadFeedback">{{ loading?'正在读取…':'刷新反馈' }}</button></section>
    <p v-if="loading" role="status">正在读取反馈…</p>
    <p v-else-if="error" class="error" role="alert">{{ error }}</p>
    <div v-else-if="!items.length" class="panel empty"><h2>还没有已确认反馈</h2><p>教师整理并确认后，你就能在这里查看。</p></div>
    <article v-for="item in error||loading?[]:items" :key="item.id" class="feedback-card"><div class="row-head"><h2>{{ item.topic }}</h2><span class="status confirmed">教师已确认</span></div><time v-if="item.confirmedAt" :datetime="item.confirmedAt">{{ item.confirmedAt.replace('T',' ').slice(0,16) }}</time><p class="feedback-body">{{ item.content }}</p></article>
    <footer>此页面是无账号的本地演示，请使用虚构数据；正式项目需要学员身份和访问权限。</footer>
  </main>
</template>
