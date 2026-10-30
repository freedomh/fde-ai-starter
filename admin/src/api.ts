export interface Student {id:number;name:string}
export interface Lesson {id:number;studentId:number;studentName:string;topic:string;performance:string;homework:string;createdAt:string}
export interface Feedback {id:number;lessonId:number;studentId:number;studentName:string;topic:string;content:string;status:'DRAFT'|'CONFIRMED';createdAt:string;confirmedAt:string|null}
export interface Stats {students:number;lessons:number;drafts:number;confirmed:number}
export async function request<T>(path:string,options?:RequestInit):Promise<T>{
  let response:Response
  try { response=await fetch('/api'+path,{...options,headers:{'Content-Type':'application/json'}}) }
  catch { throw new Error('连接失败，请检查本地服务后重试') }
  if(!response.ok){const body=await response.json().catch(()=>({}));throw new Error(body.message||'操作未完成，请重新读取后重试')}
  return response.json()
}
export const send=<T>(path:string,body?:unknown,method='POST')=>request<T>(path,{method,...(body===undefined?{}:{body:JSON.stringify(body)})})
