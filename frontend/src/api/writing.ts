import { http } from '@/api/http'
import type { ContentReview } from '@/api/account'

export type WritingTag = { id:number; name:string; slug:string; dimension:string }
export type WritingResource = { id:number; resourceKind:string; expressionLevel?:string|null; title:string; slug:string; summary:string; bodyMarkdown:string; coverMediaId?:number|null; coverUrl?:string|null; cefrLevel:string; wordMin?:number|null; wordMax?:number|null; estimatedMinutes:number; templateSchemaJson?:string|null; publishStatus:string; sortOrder:number; publishedAt?:string|null; updatedAt?:string|null; tags:WritingTag[] }
export type WritingResourceSummary = Omit<WritingResource,'bodyMarkdown'|'coverMediaId'|'wordMin'|'wordMax'|'templateSchemaJson'|'publishedAt'>
export type WritingPrompt = { id:number; title:string; slug:string; summary:string; backgroundMarkdown:string; requirementsMarkdown:string; cefrLevel:string; wordMin:number; wordMax:number; estimatedMinutes:number; rubricJson?:string|null; checklistJson?:string|null; templateResourceId?:number|null; modelResourceId?:number|null; coverMediaId?:number|null; coverUrl?:string|null; publishStatus:string; sortOrder:number; publishedAt?:string|null; updatedAt?:string|null; tags:WritingTag[] }
export type WritingPromptSummary = Omit<WritingPrompt,'backgroundMarkdown'|'requirementsMarkdown'|'rubricJson'|'checklistJson'|'templateResourceId'|'modelResourceId'|'coverMediaId'|'publishedAt'>
export type WritingPage<T> = {items:T[];page:number;pageSize:number;total:number;totalPages:number}
export type WritingResourcePayload = Omit<WritingResource,'id'|'slug'|'coverUrl'|'publishStatus'|'sortOrder'|'publishedAt'|'updatedAt'|'tags'> & {slug?:string;sortOrder?:number;tagIds:number[]}
export type WritingPromptPayload = Omit<WritingPrompt,'id'|'slug'|'coverUrl'|'publishStatus'|'sortOrder'|'publishedAt'|'updatedAt'|'tags'> & {slug?:string;sortOrder?:number;tagIds:number[]}
// `http` already has `/api/v1` as baseURL. Keeping these relative is crucial:
// an absolute `/api/v1/...` here becomes `/api/v1/api/v1/...` in Axios.
const admin='/admin/english/writing'; const pub='/public/english/writing'
export const fetchWritingResources=(params:Record<string,unknown>)=>http.get<WritingPage<WritingResourceSummary>>(`${admin}/resources`,{params}).then(r=>r.data)
export const getWritingResource=(id:number)=>http.get<WritingResource>(`${admin}/resources/${id}`).then(r=>r.data)
export const createWritingResource=(data:WritingResourcePayload)=>http.post<WritingResource>(`${admin}/resources`,data).then(r=>r.data)
export const updateWritingResource=(id:number,data:WritingResourcePayload)=>http.put<WritingResource|ContentReview>(`${admin}/resources/${id}`,data).then(r=>r.data)
export const deleteWritingResource=(id:number)=>http.delete(`${admin}/resources/${id}`)
export const publishWritingResource=(id:number)=>http.post<WritingResource>(`${admin}/resources/${id}/publish`).then(r=>r.data)
export const withdrawWritingResource=(id:number)=>http.post<WritingResource>(`${admin}/resources/${id}/withdraw`).then(r=>r.data)
export const moveWritingResource=(id:number,targetIndex:number)=>http.post(`${admin}/resources/${id}/move`,{targetIndex})
export const fetchWritingPrompts=(params:Record<string,unknown>)=>http.get<WritingPage<WritingPromptSummary>>(`${admin}/prompts`,{params}).then(r=>r.data)
export const getWritingPrompt=(id:number)=>http.get<WritingPrompt>(`${admin}/prompts/${id}`).then(r=>r.data)
export const createWritingPrompt=(data:WritingPromptPayload)=>http.post<WritingPrompt>(`${admin}/prompts`,data).then(r=>r.data)
export const updateWritingPrompt=(id:number,data:WritingPromptPayload)=>http.put<WritingPrompt|ContentReview>(`${admin}/prompts/${id}`,data).then(r=>r.data)
export const deleteWritingPrompt=(id:number)=>http.delete(`${admin}/prompts/${id}`)
export const publishWritingPrompt=(id:number)=>http.post<WritingPrompt>(`${admin}/prompts/${id}/publish`).then(r=>r.data)
export const withdrawWritingPrompt=(id:number)=>http.post<WritingPrompt>(`${admin}/prompts/${id}/withdraw`).then(r=>r.data)
export type WritingExercise = {id:number;articleId:number;questionType:string;promptMarkdown:string;config:Record<string,unknown>;explanationMarkdown?:string|null;scoreValue:number;sortOrder:number;publishStatus:string;updatedAt:string}
export type WritingExercisePayload = {questionType:string;promptMarkdown:string;configJson:string;explanationMarkdown?:string|null;scoreValue:number;publishStatus?:string}
export const fetchWritingExercises=(promptId:number)=>http.get<WritingExercise[]>(`${admin}/prompts/${promptId}/exercises`).then(r=>r.data)
export const createWritingExercise=(promptId:number,data:WritingExercisePayload)=>http.post<WritingExercise>(`${admin}/prompts/${promptId}/exercises`,data).then(r=>r.data)
export const updateWritingExercise=(promptId:number,id:number,data:WritingExercisePayload)=>http.put<WritingExercise>(`${admin}/prompts/${promptId}/exercises/${id}`,data).then(r=>r.data)
export const deleteWritingExercise=(promptId:number,id:number)=>http.delete(`${admin}/prompts/${promptId}/exercises/${id}`)
export const moveWritingExercise=(promptId:number,id:number,targetIndex:number)=>http.post(`${admin}/prompts/${promptId}/exercises/${id}/move`,{targetIndex})
export const fetchPublicWritingResources=(params:Record<string,unknown>)=>http.get<WritingPage<WritingResourceSummary>>(`${pub}/resources`,{params}).then(r=>r.data)
export const fetchPublicWritingResource=(slug:string)=>http.get<WritingResource>(`${pub}/resources/${slug}`).then(r=>r.data)
export const fetchPublicWritingPrompts=(params:Record<string,unknown>)=>http.get<WritingPage<WritingPromptSummary>>(`${pub}/prompts`,{params}).then(r=>r.data)
export const fetchPublicWritingPrompt=(slug:string)=>http.get<WritingPrompt>(`${pub}/prompts/${slug}`).then(r=>r.data)
export const fetchPublicWritingExercises=(slug:string)=>http.get<Array<{id:number;questionType:string;promptMarkdown:string;config:Record<string,unknown>;scoreValue:number;sortOrder:number}>>(`${pub}/prompts/${slug}/exercises`).then(r=>r.data)
export const checkWritingExercises=(slug:string,answers:Array<{exerciseId:number;answer:unknown}>)=>http.post<{score:number;total:number;items:Array<{exerciseId:number;correct:boolean;earned:number;possible:number;explanationMarkdown?:string|null}>}>(`${pub}/prompts/${slug}/check`,{answers}).then(r=>r.data)
