export function beginOrderDrag(event,id,allowed=true){
  if(!allowed || event.target?.closest?.('button,input,select,textarea,a')){event.preventDefault();return ''}
  if(event.dataTransfer){event.dataTransfer.effectAllowed='move';event.dataTransfer.setData('text/plain',String(id))}
  return String(id)
}
// Use the original target index so dragging down can reach the final position.
export function moveOrder(ids,source,target){
  const from=ids.indexOf(String(source)),to=ids.indexOf(String(target))
  if(from<0 || to<0 || from===to)return null
  const result=[...ids];result.splice(from,1);result.splice(to,0,String(source));return result
}
