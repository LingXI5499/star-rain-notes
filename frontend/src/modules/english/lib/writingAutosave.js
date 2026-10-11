// One in-flight save. A newer local edit uses the CAS version returned by the previous request.
export function createWritingAutosave({ read, save, applyVersion, onState, delay=1000 }) {
 let timer,flight,dirty=false,stopped=false,disposed=false
 async function flush() {
  clearTimeout(timer)
  if (stopped || disposed) return false
  if (flight) { await flight; if(stopped || disposed)return false }
  if (!dirty) return true
  dirty=false;const payload=JSON.parse(JSON.stringify(read()));onState('saving')
  flight=(async()=>{
   try { const saved=await save(payload);if(!disposed){applyVersion(saved);onState('saved')}return true }
   catch(cause) { dirty=true;stopped=true;if(!disposed)onState(cause?.response?.status===409?'conflict':'error',cause);return false }
   finally { flight=null }
  })()
  const okay=await flight
  if(okay && dirty && !disposed) return flush()
  return okay
 }
 return {
  change() { if(disposed)return;dirty=true;if(stopped)return;onState('dirty');clearTimeout(timer);timer=setTimeout(flush,delay) },
  flush,
  stop() { stopped=true;clearTimeout(timer) },
  resume() { stopped=false;return flush() },
  dispose() { disposed=true;clearTimeout(timer) },
  isDirty:()=>!disposed && (dirty || Boolean(flight)),
  isStopped:()=>stopped,
 }
}
