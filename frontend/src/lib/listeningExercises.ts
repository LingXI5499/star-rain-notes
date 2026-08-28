export interface ListeningQuestionType {
  value: string
  label: string
  hint: string
  defaultConfig: Record<string, unknown>
}

const choice = { options: [{ key: 'a', text: '选项 A' }, { key: 'b', text: '选项 B' }], answer: 'a' }

export const listeningQuestionTypes: ListeningQuestionType[] = [
  { value: 'PHONEME_WORD', label: '辨音选词', hint: '从选项中选择听到的单词。', defaultConfig: choice },
  { value: 'MINIMAL_PAIR', label: '最小对立体', hint: 'pair 必须包含两个单词，answer 必须是其中之一。', defaultConfig: { pair: ['ship', 'sheep'], answer: 'sheep' } },
  { value: 'LINKING_FILL', label: '连读填空', hint: '填写连读后听到的内容。', defaultConfig: { answer: 'an apple' } },
  { value: 'WEAK_FORM_FILL', label: '弱读填空', hint: '填写弱读词或短语。', defaultConfig: { answer: 'to' } },
  { value: 'INFO_FILL', label: '信息填空', hint: '填写材料中的关键信息。', defaultConfig: { answer: '标准答案' } },
  { value: 'INFO_CHOICE', label: '信息选择', hint: '根据材料选择正确答案。', defaultConfig: choice },
  { value: 'NUMBER_FILL', label: '数字填空', hint: '填写听到的数字。', defaultConfig: { answer: '42' } },
  { value: 'TIME_FILL', label: '时间填空', hint: '填写听到的时间。', defaultConfig: { answer: '9:30' } },
  { value: 'LOCATION_FILL', label: '地点填空', hint: '填写听到的地点。', defaultConfig: { answer: 'the library' } },
  { value: 'TRUE_FALSE', label: '正误判断', hint: 'answer 必须为 true 或 false。', defaultConfig: { answer: true } },
  { value: 'SEGMENT_ORDERING', label: '片段排序', hint: 'items 按正确顺序填写。', defaultConfig: { items: ['第一段', '第二段'] } },
  { value: 'MAIN_IDEA', label: '主旨选择', hint: '选择材料的主要观点。', defaultConfig: choice },
  { value: 'SPEAKER_ATTITUDE', label: '态度判断', hint: '选择说话者的态度。', defaultConfig: choice },
  { value: 'LOGIC_JUDGE', label: '逻辑判断', hint: '根据材料逻辑选择结论。', defaultConfig: choice },
  { value: 'DICTATION', label: '听写', hint: 'answer 或 answers 填写标准文本。', defaultConfig: { answer: '标准听写文本' } },
]

export function questionTypeOf(value: string): ListeningQuestionType {
  return listeningQuestionTypes.find((item) => item.value === value) ?? {
    value,
    label: value,
    hint: '历史题型，请检查配置。',
    defaultConfig: { answer: '标准答案' },
  }
}

export function defaultListeningConfig(value: string): string {
  return JSON.stringify(questionTypeOf(value).defaultConfig, null, 2)
}

export function parseListeningConfig(value: string): Record<string, unknown> {
  const parsed: unknown = JSON.parse(value)
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('配置必须是 JSON 对象。')
  }
  return parsed as Record<string, unknown>
}
