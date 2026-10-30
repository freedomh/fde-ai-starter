<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { request, send, type Student, type Lesson, type Feedback, type Stats } from './api'
const students=ref<Student[]>([])
const lessons=ref<Lesson[]>([])
const feedbacks=ref<Feedback[]>([])
const stats=ref<Stats|null>(null)
const studentId=ref<number|null>(null)
const studentName=ref('')
const topic=ref('')
const performance=ref('')
const homework=ref('')
const loading=ref(true)
const busy=ref('')
const error=ref('')
const notice=ref('')
const selected=ref<Feedback|null>(null)
const content=ref('')
const dirty=computed(()=>selected.value?.status==='DRAFT'&&content.value!==selected.value.content)
const disabled=computed(()=>loading.value||busy.value!=='')
const failure=(cause:unknown)=>cause instanceof Error?cause.message:'操作失败，请重试'
async function load(){
  const [a,b,c,d]=await Promise.all([request<Student[]>('/students'),request<Lesson[]>('/lessons'),request<Feedback[]>('/feedback'),request<Stats>('/stats')])
  students.value=a;lessons.value=b;feedbacks.value=c;stats.value=d
  if(!studentId.value&&a[0])studentId.value=a[0].id
}
async function refresh(){loading.value=true;error.value='';try{await load()}catch(cause){error.value=failure(cause)}finally{loading.value=false}}
async function addStudent(){
  if(disabled.value)return
  if(!studentName.value.trim()){error.value='请填写演示学员姓名';return}
  busy.value='student';error.value='';notice.value=''
  try{const result=await send<Student>('/students',{name:studentName.value});studentName.value='';studentId.value=result.id;notice.value='学员已添加';await load()}
  catch(cause){error.value=failure(cause)}finally{busy.value=''}
}
async function saveLesson(){
  if(disabled.value)return
  if(!studentId.value||!topic.value.trim()||!performance.value.trim()){error.value='请选择学员并填写主题和课堂表现';return}
  busy.value='lesson';error.value='';notice.value=''
  try{await send<Lesson>('/lessons',{studentId:studentId.value,topic:topic.value,performance:performance.value,homework:homework.value});topic.value='';performance.value='';homework.value='';notice.value='课堂记录已保存，可以生成反馈草稿';await load()}
  catch(cause){error.value=failure(cause)}finally{busy.value=''}
}
function feedbackFor(id:number){return feedbacks.value.find(item=>item.lessonId===id)}
function selectFeedback(item:Feedback){
  if(dirty.value){error.value='当前草稿有未保存修改，请先保存或还原';return}
  selected.value=item;content.value=item.content;error.value=''
}
async function generate(lesson:Lesson){
  if(disabled.value)return
  if(dirty.value){error.value='当前草稿有未保存修改，请先保存或还原';return}
  busy.value='generate';error.value='';notice.value=''
  try{const result=await send<Feedback>('/lessons/'+lesson.id+'/feedback');selected.value=result;content.value=result.content;notice.value='已按本地模板整理草稿，请教师检查和修改';await load()}
  catch(cause){error.value=failure(cause)}finally{busy.value=''}
}
async function saveFeedback(confirm=false){
  if(disabled.value||!selected.value||selected.value.status!=='DRAFT')return
  if(!content.value.trim()){error.value='反馈内容不能为空';return}
  busy.value=confirm?'confirm':'feedback';error.value='';notice.value=''
  let saved=false
  try{
    const draft=await send<Feedback>('/feedback/'+selected.value.id,{content:content.value},'PUT')
    selected.value=draft;content.value=draft.content;saved=true
    if(confirm){selected.value=await send<Feedback>('/feedback/'+draft.id+'/confirm');notice.value='反馈已确认，学员端现在可以查看'}
    else notice.value='草稿已保存，尚未对学员显示'
    await load()
  }catch(cause){error.value=(saved&&confirm?'草稿已保存，后续操作未完成：':'')+failure(cause)}finally{busy.value=''}
}
onMounted(refresh)
</script>

<template>
  <main>
    <header><div class="brand">课程反馈台</div><span>教师工作台 · 本地演示 / 虚构数据</span></header>
    <section class="intro"><h1>把课堂观察，变成有用的反馈。</h1><p>记录表现，整理草稿，检查确认后再让学员看到。</p></section>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" role="status">正在读取课堂记录…</p>
    <dl v-if="stats" class="stats"><div><dt>演示学员</dt><dd>{{ stats.students }}</dd></div><div><dt>课堂记录</dt><dd>{{ stats.lessons }}</dd></div><div><dt>待确认反馈</dt><dd>{{ stats.drafts }}</dd></div><div><dt>已确认反馈</dt><dd>{{ stats.confirmed }}</dd></div></dl>
    <div class="grid">
      <div>
        <section class="panel"><h2>记录本次课堂</h2>
          <form @submit.prevent="saveLesson">
            <label for="student">演示学员</label><select id="student" v-model="studentId" :disabled="disabled" required><option v-for="item in students" :key="item.id" :value="item.id">{{ item.name }}</option></select>
            <label for="topic">授课主题</label><input id="topic" v-model="topic" :disabled="disabled" maxlength="100" placeholder="例如：分数应用题" required>
            <label for="performance">课堂表现</label><textarea id="performance" v-model="performance" :disabled="disabled" maxlength="2000" placeholder="写清楚观察到的表现和需要改进的地方" required></textarea>
            <label for="homework">课后任务（可选）</label><textarea id="homework" v-model="homework" :disabled="disabled" maxlength="1000" rows="2" placeholder="下次课前，学员可以做什么？"></textarea>
            <div class="actions"><button :disabled="disabled">{{ busy==='lesson'?'保存中…':'保存课堂记录' }}</button><button type="button" class="secondary" @click="refresh" :disabled="disabled||dirty">重新读取</button></div>
          </form>
        </section>
        <section class="panel"><h2>新增演示学员</h2><form class="inline" @submit.prevent="addStudent"><div style="flex:1"><label for="new-student">学员姓名</label><input id="new-student" v-model="studentName" :disabled="disabled" maxlength="40" placeholder="使用虚构姓名" required></div><button :disabled="disabled">{{ busy==='student'?'添加中…':'添加学员' }}</button></form></section>
      </div>
      <div>
        <section v-if="selected" class="panel" aria-labelledby="editor-title">
          <div class="row-head"><h2 id="editor-title">{{ selected.status==='DRAFT'?'检查反馈草稿':'已确认反馈' }}</h2><span :class="['status',{confirmed:selected.status==='CONFIRMED'}]">{{ selected.status==='DRAFT'?'待确认':'已确认' }}</span></div>
          <p class="muted">{{ selected.studentName }} · {{ selected.topic }} · 本地模板整理</p>
          <template v-if="selected.status==='DRAFT'"><label for="feedback-content">反馈内容</label><textarea id="feedback-content" class="feedback-editor" v-model="content" :disabled="disabled" maxlength="4000"></textarea><p class="muted">教师确认前，学员端不会看到这份草稿。</p><div class="actions"><button type="button" class="secondary" :disabled="disabled" @click="saveFeedback(false)">保存草稿</button><button type="button" :disabled="disabled" @click="saveFeedback(true)">{{ busy==='confirm'?'确认中…':'保存并确认' }}</button><button v-if="dirty" type="button" class="link" :disabled="disabled" @click="content=selected.content">还原草稿</button></div></template>
          <template v-else><p class="feedback-body">{{ selected.content }}</p><p class="muted">已向学员显示，当前演示流程不再编辑已确认内容。</p></template>
        </section>
        <section class="panel"><h2>课堂记录</h2><p v-if="!loading&&!lessons.length" class="empty">还没有课堂记录。先在左侧记录一次课堂表现。</p><article v-for="lesson in lessons" :key="lesson.id" class="lesson"><div class="row-head"><h3>{{ lesson.topic }}</h3><span :class="['status',{confirmed:feedbackFor(lesson.id)?.status==='CONFIRMED'}]">{{ feedbackFor(lesson.id)?.status==='CONFIRMED'?'已确认':feedbackFor(lesson.id)?'草稿':'待整理' }}</span></div><p>{{ lesson.studentName }} · {{ lesson.createdAt.replace('T',' ').slice(0,16) }}</p><p>{{ lesson.performance }}</p><button v-if="!feedbackFor(lesson.id)" type="button" class="link" :disabled="disabled" @click="generate(lesson)">生成反馈草稿（本地模板）</button><button v-else type="button" class="link" :disabled="disabled" @click="selectFeedback(feedbackFor(lesson.id)!)">{{ feedbackFor(lesson.id)?.status==='DRAFT'?'检查与修改草稿':'查看已确认反馈' }}</button></article></section>
      </div>
    </div>
    <footer>课堂练习仅使用虚构信息。本地模板整理不调用外部模型；发布前须由教师检查确认。</footer>
  </main>
</template>
