import { catalog, themes, grammar, topics, allTags } from './content';
export interface PageRoute { route: string; kind: string; slug?: string; course?: string; page?: number; }
export const pageSize = 10;
export function getRoutes(): PageRoute[] {
  const routes: PageRoute[] = [
    {route:'tutorials',kind:'tutorials'}, {route:'blog',kind:'blog'}, {route:'blog/archive',kind:'archive'},
    {route:'portfolio',kind:'portfolio'}, {route:'english',kind:'english'}, {route:'english/vocabulary',kind:'vocabulary'},
    {route:'english/grammar',kind:'grammar'}, {route:'about',kind:'about'},
    {route:'search',kind:'search'}, {route:'privacy',kind:'privacy'}, {route:'terms',kind:'terms'},
  ];
  for(let page=2;page<=Math.ceil(catalog.blogs.length/pageSize);page++) routes.push({route:`blog/page/${page}`,kind:'blog',page});
  catalog.blogs.forEach(post=>routes.push({route:post.path.slice(1,-1),kind:'post',slug:post.slug}));
  topics.forEach(topic=>routes.push({route:`blog/topics/${topic.slug}`,kind:'topic',slug:topic.slug}));
  allTags.forEach(tag=>routes.push({route:`blog/tags/${tag.slug}`,kind:'tag',slug:tag.slug}));
  catalog.courses.forEach(course=>{routes.push({route:course.path.slice(1,-1),kind:'course',slug:course.slug});course.groups.forEach(group=>group.chapters.forEach(chapter=>routes.push({route:chapter.path.slice(1,-1),kind:'chapter',slug:chapter.slug,course:course.slug})))});
  catalog.works.forEach(work=>routes.push({route:work.path.slice(1,-1),kind:'work',slug:work.slug}));
  themes.forEach(theme=>routes.push({route:theme.path.slice(1,-1),kind:'theme',slug:theme.id}));
  grammar.sections.forEach(section=>section.lessons.forEach(lesson=>routes.push({route:lesson.path.slice(1,-1),kind:'lesson',slug:lesson.slug})));
  return routes;
}
