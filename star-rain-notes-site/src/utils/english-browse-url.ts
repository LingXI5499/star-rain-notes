export function browseURL(href:string,current:string){
  const url=new URL(href,current),previous=new URL(current);
  for(const key of ['q','size','display'])if(previous.searchParams.has(key))url.searchParams.set(key,previous.searchParams.get(key)!);
  url.searchParams.delete('page');
  return url;
}
