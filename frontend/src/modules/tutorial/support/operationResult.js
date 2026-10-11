/**
 * operationResult.js：教程与学习模块的业务支持工具。
 * 职责范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 阅读入口：isReviewPending（判断是否审核待处理）；operationNotice（操作Notice的处理函数）。
 */

/**
 * 判断是否审核待处理。
 * @param result 结果；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const isReviewPending = result => result?.reviewPending === true
/**
 * 操作Notice的处理函数。
 * 主要协作：isReviewPending。
 * @param result 结果；默认值、解构字段与可选参数见函数签名。
 * @param success = '操作成功。' success；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
export const operationNotice = (result, success = '操作成功。') => isReviewPending(result)
  ? '已提交超级管理员审核，通过后才会执行；当前内容保持原状态。'
  : success

export const operationLabels = {
  createCategory: '新增知识体系', updateCategory: '修改知识体系', deleteCategory: '删除知识体系', reorderCategories: '调整知识体系顺序',
  createTutorial: '新增教程', updateTutorial: '修改教程', deleteTutorial: '删除教程', reorderTutorials: '调整教程顺序',
  createGroup: '新增分组', updateGroup: '修改分组', deleteGroup: '删除分组', reorderGroups: '调整分组顺序',
  createChapter: '新增章节', updateChapter: '修改章节', updateChapterBody: '修改正文', deleteChapter: '删除章节', reorderChapters: '调整章节顺序',
  publish: '发布教程', withdraw: '撤回教程', publishChapter: '发布章节', withdrawChapter: '撤回章节',
  createCard: '新增知识卡片', updateCard: '修改知识卡片', deleteCard: '删除知识卡片', reorderCards: '调整知识卡片顺序',
  createQuestion: '新增章节问题', updateQuestion: '修改章节问题', deleteQuestion: '删除章节问题', reorderQuestions: '调整章节问题顺序',
}
