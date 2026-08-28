import { createHash } from 'node:crypto'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const rootDir = resolve(scriptDir, '..')
const legacySqlPath = join(rootDir, 'yulanlin', 'tutorial-taxonomy-data.sql')
const outputDir = join(rootDir, 'release', 'database-content-reset')
const sqlPath = join(outputDir, 'star-rain-content-reset.sql')
const readmePath = join(outputDir, 'README-宝塔导入说明.md')
const checksumsPath = join(outputDir, 'SHA256SUMS.txt')

const sqlString = (value) => {
  if (value === null || value === undefined) return 'NULL'
  return `'${String(value).replaceAll('\\', '\\\\').replaceAll("'", "''")}'`
}

function splitSqlTuples(valuesSql) {
  const tuples = []
  let start = -1
  let depth = 0
  let quoted = false

  for (let index = 0; index < valuesSql.length; index += 1) {
    const char = valuesSql[index]
    const next = valuesSql[index + 1]

    if (quoted) {
      if (char === '\\') {
        index += 1
      } else if (char === "'" && next === "'") {
        index += 1
      } else if (char === "'") {
        quoted = false
      }
      continue
    }

    if (char === "'") {
      quoted = true
    } else if (char === '(') {
      if (depth === 0) start = index + 1
      depth += 1
    } else if (char === ')') {
      depth -= 1
      if (depth === 0 && start >= 0) {
        tuples.push(valuesSql.slice(start, index))
        start = -1
      }
    }
  }

  if (quoted || depth !== 0) throw new Error('Legacy SQL tutorial_node VALUES is not balanced')
  return tuples
}

function splitSqlFields(tupleSql) {
  const fields = []
  let fieldStart = 0
  let quoted = false

  for (let index = 0; index < tupleSql.length; index += 1) {
    const char = tupleSql[index]
    const next = tupleSql[index + 1]

    if (quoted) {
      if (char === '\\') {
        index += 1
      } else if (char === "'" && next === "'") {
        index += 1
      } else if (char === "'") {
        quoted = false
      }
    } else if (char === "'") {
      quoted = true
    } else if (char === ',') {
      fields.push(tupleSql.slice(fieldStart, index).trim())
      fieldStart = index + 1
    }
  }

  fields.push(tupleSql.slice(fieldStart).trim())
  return fields
}

function decodeSqlValue(raw) {
  if (raw === 'NULL') return null
  if (!raw.startsWith("'") || !raw.endsWith("'")) return Number(raw)
  return raw
    .slice(1, -1)
    .replaceAll("''", "'")
    .replace(/\\'/g, "'")
    .replace(/\\\\/g, '\\')
}

function loadJavaSeCurriculum() {
  const source = readFileSync(legacySqlPath, 'utf8')
  const marker = 'INSERT INTO `tutorial_node` VALUES '
  const markerIndex = source.indexOf(marker)
  if (markerIndex < 0) throw new Error(`Cannot find tutorial_node INSERT in ${legacySqlPath}`)

  const valuesSql = source.slice(markerIndex + marker.length).replace(/;\s*$/, '')
  const nodes = splitSqlTuples(valuesSql)
    .map(splitSqlFields)
    .filter((fields) => Number(decodeSqlValue(fields[1])) === 7)
    .map((fields) => ({
      legacyId: Number(decodeSqlValue(fields[0])),
      parentLegacyId: decodeSqlValue(fields[2]),
      type: decodeSqlValue(fields[3]),
      title: decodeSqlValue(fields[4]),
      slug: decodeSqlValue(fields[5]),
      sortOrder: Number(decodeSqlValue(fields[9])),
    }))

  const groups = nodes
    .filter((node) => node.type === 'GROUP')
    .sort((left, right) => left.sortOrder - right.sortOrder || left.legacyId - right.legacyId)
    .map((group) => ({
      title: group.title,
      chapters: nodes
        .filter((node) => node.type === 'CHAPTER' && node.parentLegacyId === group.legacyId)
        .sort((left, right) => left.sortOrder - right.sortOrder || left.legacyId - right.legacyId)
        .map((chapter) => ({ title: chapter.title, slug: chapter.slug })),
    }))

  const chapterCount = groups.reduce((sum, group) => sum + group.chapters.length, 0)
  if (groups.length !== 19 || chapterCount !== 163) {
    throw new Error(`Unexpected JavaSE curriculum: ${groups.length} groups / ${chapterCount} chapters`)
  }
  // The approved V2 data intentionally keeps the "Java 常用 API" directory
  // reserved while the 163 canonical chapters live in the other 18 groups.
  // Preserve that authoritative 19/163 shape instead of inventing a chapter.
  const emptyGroups = groups.filter((group) => group.chapters.length === 0)
  if (emptyGroups.length !== 1 || emptyGroups[0].title !== 'Java 常用 API') {
    throw new Error(`Unexpected empty JavaSE groups: ${emptyGroups.map((group) => group.title).join(', ')}`)
  }
  const slugs = groups.flatMap((group) => group.chapters.map((chapter) => chapter.slug))
  if (slugs.some((slug) => !slug) || new Set(slugs).size !== slugs.length) {
    throw new Error('JavaSE chapter slugs are missing or duplicated')
  }
  return groups
}

const javaGroupFocus = {
  'Java 与开发环境': [
    '区分 JDK、运行环境与 JVM 的职责，理解源码、字节码和运行时之间的关系。',
    '掌握编译、启动、包组织和开发工具的基本流程，并能定位环境配置问题。',
    '以 JDK 21 作为实践基线；涉及其他版本时明确版本边界和兼容性。',
  ],
  'Java 基本语法': [
    '掌握变量、字面量、基本类型、引用类型、作用域和命名规则。',
    '理解源代码中的语法结构如何影响编译检查与运行结果。',
    '关注数值范围、精度、空引用和输入输出等常见边界。',
  ],
  '方法、类型转换与运算符': [
    '理解方法签名、参数传递、返回值和重载规则。',
    '区分自动转换与显式转换，避免精度丢失和溢出被忽略。',
    '根据优先级、短路规则和表达式类型判断运算结果。',
  ],
  '程序流程控制': [
    '使用条件、选择和循环结构表达可读、可终止的控制流程。',
    '理解 break、continue 与嵌套结构对执行路径的影响。',
    '通过边界输入和分支覆盖检查遗漏路径。',
  ],
  '数组与二维数组': [
    '理解数组定长、元素同类型、索引从零开始等基本约束。',
    '掌握初始化、遍历、参数传递和二维数组的数组嵌套模型。',
    '重点检查越界、空引用、默认值和复制语义。',
  ],
  '面向对象基础': [
    '用类描述状态和行为，用对象承载具体实例数据。',
    '理解封装、构造过程、this 引用和对象之间的协作。',
    '区分对象引用与对象本身，关注生命周期和可变状态。',
  ],
  '继承与多态': [
    '理解继承表达的 is-a 关系以及方法重写形成的动态分派。',
    '掌握访问控制、super、final、类型转换和 instanceof 的边界。',
    '优先通过稳定抽象实现替换能力，避免为复用代码滥用继承。',
  ],
  '抽象类与接口': [
    '使用抽象类表达共享状态与部分实现，使用接口表达能力契约。',
    '理解抽象方法、接口实现、默认方法和静态方法的职责。',
    '通过依赖抽象降低调用方与具体实现之间的耦合。',
  ],
  '代码块与内部类': [
    '区分静态初始化、实例初始化和构造器的执行时机。',
    '理解成员内部类、静态嵌套类、局部类和匿名类的使用边界。',
    '关注外部实例引用、变量捕获和初始化顺序。',
  ],
  'Lambda 与方法引用': [
    '理解函数式接口是 Lambda 和方法引用的目标类型。',
    '掌握参数、返回值、变量捕获与 effectively final 约束。',
    '用行为参数化减少样板代码，同时保持命名和副作用清晰。',
  ],
  'Java 常用 API': [
    '熟悉 Object、Objects、字符串、包装类型、数学和时间等基础 API。',
    '区分值相等与引用相等，关注不可变对象和空值处理。',
    '阅读 API 契约，明确返回值、异常、线程安全和版本要求。',
  ],
  '异常与泛型': [
    '理解异常层次、受检异常与非受检异常的处理责任。',
    '使用泛型在编译期表达类型约束，并理解通配符和类型擦除。',
    '避免吞掉异常、过度捕获和不安全的强制类型转换。',
  ],
  'List / Set / Map 集合体系': [
    '根据有序性、重复性、键值关系和访问模式选择集合。',
    '理解迭代、比较、哈希以及 equals/hashCode 对集合行为的影响。',
    '结合时间复杂度、内存成本和并发要求评估实现。',
  ],
  'Stream API': [
    '理解数据源、中间操作和终止操作组成的惰性处理流水线。',
    '掌握映射、过滤、归约、收集和分组等常见组合。',
    '避免复用流、依赖外部可变状态或盲目并行化。',
  ],
  'File、字符集与 IO': [
    '区分字节流与字符流，理解字符集参与编码和解码。',
    '掌握路径、文件操作、缓冲、NIO 与资源关闭的基本方式。',
    '处理部分读取、异常、权限、路径差异和大文件边界。',
  ],
  '多线程': [
    '理解线程生命周期、共享状态、原子性、可见性和有序性。',
    '掌握同步、锁、等待通知、并发工具和线程池的适用场景。',
    '通过限制共享、设置超时和正确关闭执行器降低并发风险。',
  ],
  '网络编程': [
    '理解 IP、端口、TCP、UDP 和 Socket 在通信中的分工。',
    '明确协议边界、消息 framing、超时、重试和连接释放。',
    '使用本地客户端与服务端验证正常、断连和异常输入路径。',
  ],
  'JUnit、反射、注解与动态代理': [
    '使用测试组织前置条件、执行步骤和可重复断言。',
    '理解反射读取类型元数据、注解承载元数据、代理拦截调用的机制。',
    '控制反射与代理的使用范围，保留类型安全和错误诊断能力。',
  ],
  'JavaSE 综合案例': [
    '从需求拆分领域对象、服务边界、数据输入输出和异常策略。',
    '综合使用集合、IO、面向对象与测试完成可运行的小型应用。',
    '通过分层、重构和自动化测试保持代码可维护。',
  ],
}

const compactTutorials = [
  {
    key: 'data_structures_algorithms',
    categoryKey: 'computer_science',
    title: '数据结构与算法',
    slug: 'data-structures-algorithms',
    summary: '从复杂度、基础数据结构到常用算法范式，建立可分析、可实现、可验证的算法基础。',
    sortOrder: 100,
    focus: [
      '用时间复杂度和空间复杂度分析方案随输入规模增长的成本。',
      '理解数据结构的操作、不变量与适用场景，而不是只记接口。',
      '使用边界样例、反例和复杂度复盘验证算法正确性。',
    ],
    groups: [
      ['复杂度分析', 'complexity-analysis', '掌握大 O 表示法，区分最好、平均、最坏情况以及时间和空间成本。'],
      ['数组与字符串', 'arrays-strings', '理解连续索引访问、原地修改、双指针、滑动窗口和字符串处理边界。'],
      ['链表', 'linked-lists', '掌握节点链接、哑节点、快慢指针以及插入、删除、反转的指针变化。'],
      ['栈与队列', 'stacks-queues', '理解后进先出与先进先出语义，以及单调栈、双端队列等常见扩展。'],
      ['哈希表', 'hash-tables', '理解散列、冲突、装载因子和键相等约定，识别以空间换时间的场景。'],
      ['树与二叉树', 'trees-binary-trees', '掌握树的层级结构、递归定义以及前序、中序、后序和层序遍历。'],
      ['堆与优先队列', 'heaps-priority-queues', '理解堆序性质、上浮下沉和 Top-K、动态最值等典型应用。'],
      ['图', 'graphs', '使用邻接表或邻接矩阵表示图，掌握 BFS、DFS 与连通性分析。'],
      ['排序与查找', 'sorting-searching', '比较常见排序的稳定性和复杂度，并掌握二分查找的循环不变量。'],
      ['递归与分治', 'recursion-divide-conquer', '明确递归终止条件、子问题定义和合并过程，控制调用深度。'],
      ['回溯', 'backtracking', '用选择、约束、撤销描述搜索树，并通过剪枝减少无效分支。'],
      ['贪心算法', 'greedy-algorithms', '识别局部最优选择，并用交换论证或结构性质说明正确性。'],
      ['动态规划', 'dynamic-programming', '定义状态、转移、初值与遍历顺序，并评估状态压缩的可行性。'],
      ['字符串算法', 'string-algorithms', '围绕匹配、前缀、回文和哈希理解字符串问题的状态与边界。'],
      ['算法题复盘', 'algorithm-review', '记录题意建模、错误假设、复杂度和可迁移模式，形成稳定解题流程。'],
    ],
  },
  {
    key: 'computer_organization',
    categoryKey: 'computer_science',
    title: '计算机组成原理',
    slug: 'computer-organization',
    summary: '从数据表示、处理器、指令到存储和输入输出，理解程序在硬件中的执行过程。',
    sortOrder: 200,
    focus: [
      '从指令执行和数据流动角度连接处理器、存储器与外部设备。',
      '区分功能原理与具体产品实现，使用层次结构理解性能权衡。',
      '通过位级表示、时序和访存过程解释程序行为。',
    ],
    groups: [
      ['数据表示', 'data-representation', '理解二进制、补码、定点数、浮点数和字符编码的表示范围与误差。'],
      ['CPU', 'cpu', '理解运算器、控制器、寄存器和时钟如何协同完成取指、译码与执行。'],
      ['指令系统', 'instruction-set', '掌握指令格式、寻址方式、指令周期以及软件与硬件之间的接口。'],
      ['存储系统', 'memory-system', '理解寄存器、缓存、主存和外存构成的层次结构及局部性原理。'],
      ['Cache', 'cache', '理解缓存行、映射、替换和写策略，以及命中率对访问成本的影响。'],
      ['总线', 'bus', '理解地址、数据和控制信息的传递，以及总线仲裁与同步方式。'],
      ['输入输出系统', 'io-system', '比较程序查询、中断和 DMA 等 I/O 控制方式及其处理流程。'],
    ],
  },
  {
    key: 'operating_systems',
    categoryKey: 'computer_science',
    title: '操作系统',
    slug: 'operating-systems',
    summary: '围绕进程、线程、内存、文件和设备管理，理解操作系统提供抽象与资源隔离的方式。',
    sortOrder: 300,
    focus: [
      '理解操作系统在硬件与应用之间提供的资源管理和抽象。',
      '用状态、队列、地址空间和并发关系分析系统行为。',
      '结合系统调用与观测工具验证理论模型。',
    ],
    groups: [
      ['进程与线程', 'processes-threads', '区分进程资源边界与线程执行单元，理解状态转换和上下文切换。'],
      ['CPU 调度', 'cpu-scheduling', '比较先来先服务、时间片、优先级等调度思想及响应、公平与吞吐权衡。'],
      ['内存管理', 'memory-management', '理解地址转换、分配、分页、分段和内外碎片等基础问题。'],
      ['虚拟内存', 'virtual-memory', '理解页表、缺页、页面置换和工作集如何提供大于物理内存的地址空间。'],
      ['文件系统', 'file-systems', '理解文件、目录、元数据、块分配、缓存和一致性之间的关系。'],
      ['I/O 模型', 'io-models', '比较阻塞、非阻塞、复用和异步 I/O 的等待与通知方式。'],
      ['同步与互斥', 'synchronization-mutual-exclusion', '理解临界区、锁、信号量、条件变量以及竞态条件。'],
      ['死锁', 'deadlocks', '掌握死锁必要条件以及预防、避免、检测和恢复的基本策略。'],
    ],
  },
  {
    key: 'computer_networks',
    categoryKey: 'computer_science',
    title: '计算机网络',
    slug: 'computer-networks',
    summary: '从分层协议到 HTTP、DNS 和 Socket，建立端到端通信与网络排障基础。',
    sortOrder: 400,
    focus: [
      '使用分层和封装理解数据从应用到链路再返回的过程。',
      '区分协议提供的语义、可靠性边界和安全责任。',
      '结合抓包、连通性与服务端日志定位网络问题。',
    ],
    groups: [
      ['网络分层', 'network-layering', '理解 OSI 与 TCP/IP 模型、封装解封装以及各层的职责边界。'],
      ['TCP/IP', 'tcp-ip', '理解 IP 寻址与路由、TCP 连接和可靠传输如何协同提供端到端通信。'],
      ['UDP', 'udp', '理解无连接数据报、消息边界、丢包可能性及低开销场景。'],
      ['HTTP 与 HTTPS', 'http-https', '掌握请求响应、状态码、缓存、连接复用以及 TLS 提供的机密性与身份校验。'],
      ['DNS', 'dns', '理解域名层次、递归与迭代查询、记录类型、缓存和 TTL。'],
      ['Socket', 'sockets', '理解套接字地址、监听、连接、读写、超时和关闭流程。'],
      ['网络安全', 'network-security', '认识窃听、篡改、伪造和拒绝服务风险，并采用加密、认证和最小暴露原则。'],
      ['网络故障排查', 'network-troubleshooting', '按 DNS、路由、端口、TLS、代理和应用日志分层定位问题。'],
    ],
  },
  {
    key: 'git_collaboration',
    categoryKey: 'programming_tooling',
    title: 'Git 与代码协作',
    slug: 'git-collaboration',
    summary: '掌握版本记录、分支协作、历史整理、冲突解决和代码评审的基础工作流。',
    sortOrder: 100,
    focus: [
      '理解工作区、暂存区、本地仓库和远程仓库之间的数据流。',
      '用小而清晰的提交保存可审查、可回退的历史。',
      '在共享分支操作前明确影响范围，避免破坏他人历史。',
    ],
    groups: [
      ['基本操作', 'basic-operations', '掌握 init、clone、status、add、commit、log、diff、fetch、pull 和 push 的作用。'],
      ['分支模型', 'branching-models', '理解分支指针和合并策略，并按团队规模选择短分支或主干开发。'],
      ['Commit 规范', 'commit-conventions', '让一次提交表达单一意图，并用准确标题和正文说明原因与影响。'],
      ['Merge 与 Rebase', 'merge-rebase', '区分保留分叉历史的合并与重放提交的变基，避免改写已共享历史。'],
      ['冲突解决', 'conflict-resolution', '逐段确认双方意图，完成构建和测试后再提交冲突解决结果。'],
      ['GitHub 协作', 'github-collaboration', '通过 Fork、分支、Pull Request、评审和保护规则组织协作。'],
    ],
  },
  {
    key: 'agent_architecture_development',
    categoryKey: 'agent_development',
    title: 'Agent 架构与开发',
    slug: 'agent-architecture-development',
    summary: '从模型、工具、状态与工作流出发，理解可控、可评测、可观测的智能体系统。',
    sortOrder: 100,
    focus: [
      '将智能体视为模型、指令、工具、状态和控制循环组成的系统。',
      '为外部操作设置明确输入输出、权限、超时、重试和人工确认。',
      '用可复现任务集、调用轨迹和结果指标评估系统，而非只看单次演示。',
    ],
    groups: [
      ['Agent 概念', 'agent-concepts', '理解智能体目标、环境、观察、行动与反馈循环，以及它和普通聊天调用的差别。'],
      ['Tool Calling', 'tool-calling', '使用结构化参数描述工具，校验模型输出，并隔离工具执行错误与模型错误。'],
      ['Memory', 'memory', '区分会话上下文、持久记忆与检索知识，并控制写入、更新和遗忘策略。'],
      ['Planning', 'planning', '将复杂目标拆分为可验证步骤，并在新信息和失败后重新规划。'],
      ['Workflow', 'workflow', '用确定性流程编排稳定步骤，仅在需要判断的节点引入模型决策。'],
      ['状态管理', 'state-management', '明确状态结构、生命周期、并发更新和恢复点，避免依赖隐式上下文。'],
      ['Human-in-the-Loop', 'human-in-the-loop', '在高风险、不可逆或信息不足的操作前引入人工确认和修正。'],
      ['多智能体协作', 'multi-agent-collaboration', '按职责拆分代理，并明确任务分派、共享上下文、冲突处理和终止条件。'],
      ['Agent 评测', 'agent-evaluation', '覆盖任务成功率、工具正确性、步骤效率、鲁棒性、延迟和成本。'],
      ['Agent 安全', 'agent-security', '防范提示注入、越权工具调用、敏感数据泄露和不可信输出传播。'],
      ['Agent 可观测性', 'agent-observability', '记录模型请求、工具调用、状态变化、错误和耗时，同时对敏感字段脱敏。'],
    ],
  },
  {
    key: 'software_engineering_lifecycle',
    categoryKey: 'software_engineering',
    title: '软件工程基础与生命周期',
    slug: 'software-engineering-lifecycle',
    summary: '从生命周期、开发模型和敏捷实践理解软件如何被持续定义、构建、验证和改进。',
    sortOrder: 100,
    focus: [
      '把需求、设计、实现、测试、交付和运维看作可追踪的连续活动。',
      '依据风险、反馈速度和变更成本选择过程，而不是机械套用模型。',
      '通过明确完成标准、评审、度量和复盘持续改进交付。',
    ],
    groups: [
      ['软件生命周期', 'software-lifecycle', '理解从构想到退役的阶段、交付物、参与角色和反馈关系。'],
      ['开发模型', 'development-models', '比较瀑布、迭代、增量等模型对计划、风险和变更的处理方式。'],
      ['敏捷开发', 'agile-development', '理解价值优先、短反馈周期、持续交付和拥抱变化等核心思想。'],
      ['Scrum', 'scrum', '掌握角色、事件和工件，并用透明、检查、适应推动迭代。'],
      ['软件过程改进', 'process-improvement', '从现状和瓶颈出发设定指标，通过小步实验验证改进效果。'],
    ],
  },
]

const categories = [
  ['java_fullstack', 'Java 全栈知识体系', 'java-fullstack', 100],
  ['computer_science', '计算机基础知识体系', 'computer-science', 200],
  ['programming_tooling', '通用编程与开发工具体系', 'programming-tooling', 300],
  ['agent_development', '智能体开发知识体系', 'agent-development', 400],
  ['software_engineering', '软件工程方法体系', 'software-engineering', 500],
]

const javaTutorial = {
  key: 'javase',
  categoryKey: 'java_fullstack',
  title: 'JavaSE 基础',
  slug: 'javase',
  summary: '以 JDK 21 为实践基线，系统学习 Java 语法、面向对象、集合、IO、并发、网络和常用开发能力。',
  sortOrder: 100,
  groups: loadJavaSeCurriculum(),
}

function chapterBody({ tutorial, groupTitle, chapterTitle, detail, focus }) {
  const corePoints = detail
    ? [detail, ...focus]
    : [
        `围绕“${chapterTitle}”梳理定义、语法或运行机制，并明确它在“${groupTitle}”中的位置。`,
        ...focus,
      ]

  return [
    `# ${chapterTitle}`,
    '',
    '## 学习目标',
    '',
    `- 理解“${chapterTitle}”的核心概念、适用场景与使用边界。`,
    `- 能将该知识点与“${groupTitle}”中的相邻内容建立联系。`,
    '- 能通过最小示例或可重复实验验证结论，而不是只记术语。',
    '',
    '## 核心知识',
    '',
    ...corePoints.map((point) => `- ${point}`),
    '',
    '## 实践建议',
    '',
    tutorial.slug === 'javase'
      ? '- 在独立包中编写可运行的最小示例，使用 JDK 21 完成编译和运行。'
      : '- 使用一个最小案例画出结构、记录输入输出，并说明选择当前方案的理由。',
    '- 同时检查正常路径、边界条件和失败路径；把发现的问题记录到复盘笔记。',
    '- 涉及版本、命令或安全配置时，以对应版本的官方文档为准。',
  ].join('\n')
}

function insertCategorySql([key, name, slug, sortOrder]) {
  return [
    'INSERT INTO tutorial_category (parent_id, name, slug, sort_order)',
    `VALUES (NULL, ${sqlString(name)}, ${sqlString(slug)}, ${sortOrder});`,
    `SET @category_${key} = LAST_INSERT_ID();`,
    '',
  ].join('\n')
}

function insertTutorialSql(tutorial) {
  return [
    'INSERT INTO tutorial (',
    '    category_id, title, slug, summary, cover_media_id, publish_status, sort_order,',
    '    seo_title, seo_description, published_at',
    ') VALUES (',
    `    @category_${tutorial.categoryKey}, ${sqlString(tutorial.title)}, ${sqlString(tutorial.slug)},`,
    `    ${sqlString(tutorial.summary)}, NULL, 'PUBLISHED', ${tutorial.sortOrder},`,
    `    ${sqlString(`${tutorial.title} | 星雨笔录`)}, ${sqlString(tutorial.summary)}, UTC_TIMESTAMP(3)`,
    ');',
    `SET @tutorial_${tutorial.key} = LAST_INSERT_ID();`,
    '',
  ].join('\n')
}

function insertGroupSql(tutorial, group, groupIndex, focus, compact = false) {
  const groupOrder = (groupIndex + 1) * 10
  const chapters = compact
    ? [{ title: `${group.title}：核心概念与实践`, slug: group.slug, detail: group.detail }]
    : group.chapters

  const lines = [
    'INSERT INTO tutorial_node (',
    '    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,',
    '    publish_status, sort_order, published_at',
    ') VALUES (',
    `    @tutorial_${tutorial.key}, NULL, 'GROUP', ${sqlString(group.title)}, NULL, NULL, NULL,`,
    `    NULL, ${groupOrder}, NULL`,
    ');',
    'SET @curriculum_group_id = LAST_INSERT_ID();',
    '',
  ]

  chapters.forEach((chapter, chapterIndex) => {
    const body = chapterBody({
      tutorial,
      groupTitle: group.title,
      chapterTitle: chapter.title,
      detail: chapter.detail,
      focus,
    })
    const summary = `学习${chapter.title}的核心概念、使用边界与实践方法。`
    lines.push(
      'INSERT INTO tutorial_node (',
      '    tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,',
      '    publish_status, sort_order, published_at',
      ') VALUES (',
      `    @tutorial_${tutorial.key}, @curriculum_group_id, 'CHAPTER', ${sqlString(chapter.title)},`,
      `    ${sqlString(chapter.slug)}, ${sqlString(summary)}, ${sqlString(body)},`,
      `    'PUBLISHED', ${(chapterIndex + 1) * 10}, UTC_TIMESTAMP(3)`,
      ');',
      '',
    )
  })

  return lines.join('\n')
}

function buildSql() {
  const allTutorials = [javaTutorial, ...compactTutorials]
  const compactGroupCount = compactTutorials.reduce((sum, tutorial) => sum + tutorial.groups.length, 0)
  const expectedGroups = javaTutorial.groups.length + compactGroupCount
  const expectedChapters = javaTutorial.groups.reduce((sum, group) => sum + group.chapters.length, 0) + compactGroupCount
  if (expectedGroups !== 79 || expectedChapters !== 223) {
    throw new Error(`Unexpected final curriculum: ${expectedGroups} groups / ${expectedChapters} chapters`)
  }

  const lines = [
    '-- 星雨笔录：教程与作品内容重建脚本',
    '-- 适用结构：Flyway V25 / MySQL 8.0、8.4',
    '-- 作用范围：重建 tutorial_category、tutorial、tutorial_node、portfolio_project。',
    '-- 保留范围：博客、英语、用户、媒体与 flyway_schema_history 均不修改。',
    '-- 重要：执行前必须完成数据库备份；不要使用 mysql --force。',
    '',
    'SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;',
    'SET time_zone = "+00:00";',
    '',
    '-- 保存非目标内容数量，事务提交前会再次校验。',
    'SET @before_blog_post = (SELECT COUNT(*) FROM blog_post);',
    'SET @before_vocabulary_theme = (SELECT COUNT(*) FROM vocabulary_theme);',
    'SET @before_vocabulary_word = (SELECT COUNT(*) FROM vocabulary_word);',
    'SET @before_grammar_lesson = (SELECT COUNT(*) FROM english_grammar_lesson);',
    'SET @before_reading_article = (SELECT COUNT(*) FROM english_reading_article);',
    'SET @before_listening_item = (SELECT COUNT(*) FROM english_listening_item);',
    'SET @before_writing_prompt = (SELECT COUNT(*) FROM english_writing_prompt);',
    '',
    'START TRANSACTION;',
    '',
    '-- 清理仅与教程/作品相关的引用；博客和英语审核记录保留。',
    "DELETE FROM content_review_request WHERE content_type = 'TUTORIAL_CHAPTER';",
    'DELETE FROM profile_selected_content',
    'WHERE tutorial_id IS NOT NULL OR portfolio_project_id IS NOT NULL;',
    '',
    '-- 按外键依赖顺序清空教程体系与作品。',
    "DELETE FROM tutorial_node WHERE node_type = 'CHAPTER';",
    "DELETE FROM tutorial_node WHERE node_type = 'GROUP';",
    'DELETE FROM tutorial;',
    'DELETE FROM tutorial_category;',
    'DELETE FROM portfolio_project;',
    '',
    '-- 五大知识体系。',
  ]

  categories.forEach((category) => lines.push(insertCategorySql(category)))

  lines.push('-- 八门公开教程。')
  allTutorials.forEach((tutorial) => lines.push(insertTutorialSql(tutorial)))

  lines.push('-- JavaSE：沿用 V2 权威目录中的 19 组、163 个章节标题与 slug。')
  javaTutorial.groups.forEach((group, index) => {
    const focus = javaGroupFocus[group.title]
    if (!focus) throw new Error(`Missing JavaSE focus for group: ${group.title}`)
    lines.push(insertGroupSql(javaTutorial, group, index, focus, false))
  })

  lines.push('-- 其余教程：每个真实目录组包含一篇公开提纲章节。')
  compactTutorials.forEach((tutorial) => {
    tutorial.groups.forEach(([title, slug, detail], index) => {
      lines.push(insertGroupSql(tutorial, { title, slug, detail }, index, tutorial.focus, true))
    })
  })

  const techStack = JSON.stringify([
    'Java 21',
    'Spring Boot 3.5',
    'MyBatis-Plus',
    'MySQL 8',
    'Flyway',
    'Vue 3',
    'TypeScript',
    'Vite',
    'Nginx',
  ])
  const portfolioSummary = '面向个人知识沉淀与学习管理的一体化系统，覆盖教程、博客、作品、英语学习、搜索与后台内容管理。'
  const portfolioBody = [
    '# 星雨笔录',
    '',
    '星雨笔录是一个前后端分离的个人知识系统，用统一的信息架构管理教程、博客、作品、英语学习内容与个人主页。',
    '',
    '## 已实现能力',
    '',
    '- 教程分类、课程目录、章节发布与前台阅读。',
    '- 博客、作品、媒体资源、个人资料和站内搜索。',
    '- 英语词汇、语法、阅读、听力、写作与学习记录。',
    '- 管理后台、账号权限、内容审核与发布状态管理。',
    '',
    '## 技术实现',
    '',
    '- 后端使用 Java 21、Spring Boot 3.5、Spring Security、MyBatis-Plus、MySQL 8 与 Flyway。',
    '- 前端使用 Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios 与 Element Plus。',
    '- 生产环境由 Nginx 提供 HTTPS 和静态资源服务，Spring Boot JAR 提供 API，MySQL 保存业务数据。',
    '',
    '## 工程实践',
    '',
    '- 数据库结构由 Flyway 按版本迁移，部署配置与业务代码分离。',
    '- 健康检查、日志轮转、备份脚本和反向代理配置随项目维护。',
    '- 公开内容与后台管理使用独立接口，并通过发布状态控制可见范围。',
  ].join('\n')

  lines.push(
    '-- 唯一公开、上线并精选的作品：星雨笔录。',
    'INSERT INTO portfolio_project (',
    '    title, slug, summary, role, tech_stack, body_markdown, cover_media_id,',
    '    repository_url, demo_url, publish_status, project_status, featured, sort_order,',
    '    started_at, completed_at, seo_title, seo_description, published_at',
    ') VALUES (',
    `    '星雨笔录', 'star-rain-notes', ${sqlString(portfolioSummary)}, '独立设计与开发',`,
    `    CAST(${sqlString(techStack)} AS JSON), ${sqlString(portfolioBody)}, NULL,`,
    "    'https://gitee.com/linglingxixixi/yulanlin-blog', 'https://yulanlin.cn',",
    "    'PUBLISHED', 'ONLINE', 1, 100, NULL, NULL,",
    `    '星雨笔录 | 个人知识与英语学习系统', ${sqlString(portfolioSummary)}, UTC_TIMESTAMP(3)`,
    ');',
    '',
    '-- 事务内硬校验：任何一项不满足都会触发 CHECK 错误，连接关闭后自动回滚。',
    'SET @content_reset_valid = (',
    '    (SELECT COUNT(*) FROM tutorial_category) = 5',
    '    AND (SELECT COUNT(*) FROM tutorial) = 8',
    "    AND (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'GROUP') = 79",
    "    AND (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER') = 223",
    "    AND (SELECT COUNT(*) FROM portfolio_project WHERE slug = 'star-rain-notes') = 1",
    '    AND (SELECT COUNT(*) FROM tutorial_node chapter',
    '         LEFT JOIN tutorial_node parent',
    '           ON parent.tutorial_id = chapter.tutorial_id AND parent.id = chapter.parent_id',
    "         WHERE chapter.node_type = 'CHAPTER'",
    "           AND (parent.id IS NULL OR parent.node_type <> 'GROUP')) = 0",
    "    AND (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER'",
    "         AND (body_markdown IS NULL OR body_markdown = ''",
    "              OR body_markdown REGEXP '待编写|待补充')) = 0",
    '    AND (SELECT COUNT(*) FROM blog_post) = @before_blog_post',
    '    AND (SELECT COUNT(*) FROM vocabulary_theme) = @before_vocabulary_theme',
    '    AND (SELECT COUNT(*) FROM vocabulary_word) = @before_vocabulary_word',
    '    AND (SELECT COUNT(*) FROM english_grammar_lesson) = @before_grammar_lesson',
    '    AND (SELECT COUNT(*) FROM english_reading_article) = @before_reading_article',
    '    AND (SELECT COUNT(*) FROM english_listening_item) = @before_listening_item',
    '    AND (SELECT COUNT(*) FROM english_writing_prompt) = @before_writing_prompt',
    ');',
    '',
    'CREATE TEMPORARY TABLE star_rain_content_reset_assert (',
    '    ok TINYINT NOT NULL,',
    '    CONSTRAINT ck_star_rain_content_reset_assert CHECK (ok = 1)',
    ');',
    'INSERT INTO star_rain_content_reset_assert (ok) VALUES (@content_reset_valid);',
    '',
    'COMMIT;',
    'DROP TEMPORARY TABLE star_rain_content_reset_assert;',
    '',
    '-- 预期结果：5 / 8 / 79 / 223 / 1，且 invalid_chapters=0。',
    'SELECT',
    '    (SELECT COUNT(*) FROM tutorial_category) AS categories,',
    '    (SELECT COUNT(*) FROM tutorial) AS tutorials,',
    "    (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'GROUP') AS curriculum_groups,",
    "    (SELECT COUNT(*) FROM tutorial_node WHERE node_type = 'CHAPTER') AS published_chapters,",
    '    (SELECT COUNT(*) FROM portfolio_project) AS portfolio_projects;',
    '',
    'SELECT COUNT(*) AS invalid_chapters',
    'FROM tutorial_node chapter',
    'LEFT JOIN tutorial_node parent',
    '  ON parent.tutorial_id = chapter.tutorial_id AND parent.id = chapter.parent_id',
    "WHERE chapter.node_type = 'CHAPTER'",
    "  AND (chapter.publish_status <> 'PUBLISHED'",
    '       OR chapter.published_at IS NULL',
    "       OR chapter.body_markdown IS NULL OR chapter.body_markdown = ''",
    "       OR chapter.body_markdown REGEXP '待编写|待补充'",
    "       OR parent.id IS NULL OR parent.node_type <> 'GROUP');",
    '',
    "SELECT c.name AS category_name, COUNT(t.id) AS published_tutorials",
    'FROM tutorial_category c',
    "LEFT JOIN tutorial t ON t.category_id = c.id AND t.publish_status = 'PUBLISHED'",
    'GROUP BY c.id, c.name, c.sort_order',
    'ORDER BY c.sort_order, c.id;',
    '',
  )

  return lines.join('\n')
}

const readme = `# 星雨笔录数据库内容重建包

本目录只负责重建教程体系与作品数据。它不会修改博客、英语、用户、媒体、数据库结构或 Flyway 迁移记录。

执行后固定得到：5 个教程分类、8 门公开教程、79 个目录组、223 个公开章节，以及 1 个公开精选作品“星雨笔录”。

> 该操作会删除数据库里现有的全部教程、教程章节和作品。必须先备份数据库。

## 一、在宝塔中备份数据库

1. 登录宝塔面板。
2. 左侧点击“数据库”。
3. 找到数据库 \`star_rain_notes\`。
4. 点击该行右侧“备份”。
5. 等待备份完成，再点击“备份列表”确认出现刚生成的备份文件。
6. 记录备份时间；没有成功备份时不要继续。

## 二、上传本目录

1. 左侧点击“文件”。
2. 进入 \`/opt/star-rain-notes/\`。如果目录不存在，先依次创建 \`star-rain-notes\` 目录。
3. 将整个 \`database-content-reset\` 文件夹上传到该目录。
4. 上传完成后应看到：

~~~text
/opt/star-rain-notes/database-content-reset/
├── star-rain-content-reset.sql
├── README-宝塔导入说明.md
└── SHA256SUMS.txt
~~~

不要把 SQL 放进网站静态目录，也不要编辑 SQL 文件编码。

## 三、校验上传文件

1. 左侧点击“终端”。
2. 执行：

~~~bash
cd /opt/star-rain-notes/database-content-reset
sha256sum -c SHA256SUMS.txt
~~~

两个文件都必须显示 \`OK\`。如果显示 \`FAILED\`，删除服务器上的文件夹后重新上传，不能继续导入。

## 四、确认后端与数据库状态

执行：

~~~bash
curl -fsS http://127.0.0.1:24680/actuator/health
mysql -u star_rain -p -D star_rain_notes -e "SELECT VERSION(); SELECT MAX(version) AS flyway_version FROM flyway_schema_history WHERE success=1;"
~~~

第一条应返回 \`{"status":"UP"}\`。第二条输入数据库用户 \`star_rain\` 的密码后，Flyway 版本应为 \`25\`。

如果健康检查失败或 Flyway 不是 25，停止操作，不要导入。

## 五、记录导入前数量

执行下面命令，输入数据库密码：

~~~bash
mysql -u star_rain -p -D star_rain_notes -e "SELECT COUNT(*) AS blogs_before FROM blog_post; SELECT COUNT(*) AS words_before FROM vocabulary_word; SELECT COUNT(*) AS grammar_before FROM english_grammar_lesson;"
~~~

保存终端输出，导入后用于核对博客和英语数据没有变化。

## 六、执行内容重建

确保当前目录仍是 \`/opt/star-rain-notes/database-content-reset\`，然后执行：

~~~bash
mysql --default-character-set=utf8mb4 --show-warnings -u star_rain -p star_rain_notes < star-rain-content-reset.sql
~~~

- 只在密码提示符中输入密码，不要把密码写进命令。
- 不要给命令添加 \`--force\`。
- 正常结束时，终端最后会显示分类、教程、目录组、章节和作品数量。
- 预期值依次为 \`5 / 8 / 79 / 223 / 1\`，\`invalid_chapters\` 必须为 \`0\`。
- 出现任何 \`ERROR\` 都视为失败；不要重复尝试，先执行第十节恢复步骤。

数据通过事务一次性提交，正常导入不需要重启 Java 项目。

## 七、再次执行数据库验收

~~~bash
mysql -u star_rain -p -D star_rain_notes -e "SELECT COUNT(*) categories FROM tutorial_category; SELECT COUNT(*) tutorials FROM tutorial; SELECT node_type,COUNT(*) total FROM tutorial_node GROUP BY node_type; SELECT title,slug,publish_status,project_status,featured FROM portfolio_project; SELECT COUNT(*) blogs_after FROM blog_post; SELECT COUNT(*) words_after FROM vocabulary_word; SELECT COUNT(*) grammar_after FROM english_grammar_lesson;"
~~~

确认：

- 分类 5、教程 8、GROUP 79、CHAPTER 223、作品 1。
- “星雨笔录”为 \`PUBLISHED / ONLINE / featured=1\`。
- 博客、词汇和语法数量与第五节完全一致。

## 八、验证公开接口

依次执行：

~~~bash
curl -fsS -o /dev/null -w "categories: %{http_code}\\n" http://127.0.0.1:24680/api/v1/public/tutorial-categories/tree
curl -fsS -o /dev/null -w "tutorials: %{http_code}\\n" http://127.0.0.1:24680/api/v1/public/tutorials
curl -fsS -o /dev/null -w "javase: %{http_code}\\n" http://127.0.0.1:24680/api/v1/public/tutorials/javase
curl -fsS -o /dev/null -w "portfolio: %{http_code}\\n" http://127.0.0.1:24680/api/v1/public/portfolio/projects
curl -fsS -o /dev/null -w "star-rain: %{http_code}\\n" http://127.0.0.1:24680/api/v1/public/portfolio/projects/star-rain-notes
~~~

五项都应返回 \`200\`。

## 九、浏览器验收

1. 打开 \`https://yulanlin.cn/tutorials\`。
2. 按 \`Ctrl+F5\` 强制刷新。
3. 确认左侧显示五个分类，全部教程合计为 8。
4. 打开“JavaSE 基础”，确认目录组和章节可以展开、章节正文可以打开。
5. 分别检查四门计算机基础教程以及 Git、Agent、软件工程教程。
6. 打开网站“作品”页面，确认仅显示精选作品“星雨笔录”，演示和仓库链接正确。

## 十、失败恢复

如果导入报错或页面验收不通过：

1. 不要手工删除其他表，也不要再次运行 SQL。
2. 回到宝塔“数据库”。
3. 找到 \`star_rain_notes\`，打开“备份列表”。
4. 选择第一节生成的备份，点击“恢复”。
5. 等待恢复结束后执行健康检查。
6. 保存导入时完整错误输出，再根据第一条 \`ERROR\` 定位问题。

SQL 导入和数据库恢复都会直接改变后端读取的数据，通常不需要重启 Java；只有健康检查异常时才在宝塔 Java 项目管理中重启。
`

mkdirSync(outputDir, { recursive: true })
writeFileSync(sqlPath, buildSql(), 'utf8')
writeFileSync(readmePath, readme.replace(/^\n/, ''), 'utf8')

const checksumLines = [sqlPath, readmePath].map((filePath) => {
  const digest = createHash('sha256').update(readFileSync(filePath)).digest('hex')
  return `${digest}  ${filePath.slice(outputDir.length + 1).replaceAll('\\', '/')}`
})
writeFileSync(checksumsPath, `${checksumLines.join('\n')}\n`, 'utf8')

console.log(`Generated ${sqlPath}`)
console.log('Expected rows: 5 categories, 8 tutorials, 79 groups, 223 chapters, 1 portfolio project')
