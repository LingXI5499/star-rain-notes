import { http } from './http'
export interface WordFamilyMember { id:number;spelling:string;partOfSpeech:string|null;phoneticUs:string|null;translation:string|null;cefrLevel:string|null;exampleSentence:string|null;exampleTranslation:string|null;sortOrder:number }
export interface WordFamily { id:number;headWord:string;slug:string;description:string|null;wordIds:number[];members:WordFamilyMember[];updatedAt:string }
export interface WordFamilyPayload { headWord:string;slug?:string;description?:string|null;wordIds:number[] }
export type WordFamilyMemberPayload = Omit<WordFamilyMember,'id'|'sortOrder'> & {sortOrder?:number}
const root='/admin/english/vocabulary/families'
export const fetchWordFamilies=(q='')=>http.get<WordFamily[]>(root,{params:{q:q||undefined}}).then(r=>r.data)
export const createWordFamily=(data:WordFamilyPayload)=>http.post<WordFamily>(root,data).then(r=>r.data)
export const updateWordFamily=(id:number,data:WordFamilyPayload)=>http.put<WordFamily>(`${root}/${id}`,data).then(r=>r.data)
export const deleteWordFamily=(id:number)=>http.delete(`${root}/${id}`)
export const addWordFamilyMember=(id:number,data:WordFamilyMemberPayload)=>http.post<WordFamilyMember>(`${root}/${id}/members`,data).then(r=>r.data)
export const updateWordFamilyMember=(id:number,memberId:number,data:WordFamilyMemberPayload)=>http.put<WordFamilyMember>(`${root}/${id}/members/${memberId}`,data).then(r=>r.data)
export const deleteWordFamilyMember=(id:number,memberId:number)=>http.delete(`${root}/${id}/members/${memberId}`)
export const moveWordFamilyMember=(id:number,memberId:number,targetIndex:number)=>http.post(`${root}/${id}/members/${memberId}/move`,{targetIndex})
