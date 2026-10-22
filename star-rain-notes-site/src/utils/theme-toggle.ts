export function themeRevealGeometry(x:number,y:number,width:number,height:number){
  const cx=Math.max(0,Math.min(width,x)),cy=Math.max(0,Math.min(height,y));
  const radius=Math.hypot(Math.max(cx,width-cx),Math.max(cy,height-cy))+20;
  return {center:`${(cx/width*100).toFixed(4)}% ${(cy/height*100).toFixed(4)}%`,radius:`${(radius/(Math.hypot(width,height)/Math.SQRT2)*100).toFixed(4)}%`};
}

export function installThemeToggle(){
  const button=document.querySelector<HTMLButtonElement>('#theme-toggle');if(!button)return;
  const root=document.documentElement;
  let switching=false;
  const sync=()=>{
    const dark=root.dataset.theme==='dark',label=dark?'切换浅色模式':'切换深色模式';
    root.style.colorScheme=dark?'dark':'light';
    button.setAttribute('data-theme-icon',dark?'dark':'light');button.setAttribute('aria-pressed',String(dark));button.setAttribute('aria-label',label);button.title=label;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content',dark?'#0c1511':'#f4f3ed');
  };
  sync();
  button.addEventListener('click',async(event)=>{
    if(switching)return;
    switching=true;button.setAttribute('aria-busy','true');
    let committed=false;
    const commit=()=>{
      if(committed)return;committed=true;
      root.dataset.theme=root.dataset.theme==='dark'?'light':'dark';
      try{localStorage.setItem('star-rain-theme',root.dataset.theme);}catch{/* The current tab still applies the choice. */}
      sync();
    };
    try{
      if(matchMedia('(prefers-reduced-motion: reduce)').matches){commit();return;}
      if(typeof document.startViewTransition!=='function'){
        commit();root.classList.add('is-theme-fading');
        await new Promise(resolve=>setTimeout(resolve,320));return;
      }
      const rect=button.getBoundingClientRect();
      const x=event.detail?event.clientX:rect.left+rect.width/2,y=event.detail?event.clientY:rect.top+rect.height/2;
      const geometry=themeRevealGeometry(x,y,innerWidth,innerHeight);
      root.style.setProperty('--theme-reveal-center',geometry.center);root.style.setProperty('--theme-reveal-radius',geometry.radius);
      root.classList.add('is-theme-revealing');
      root.dataset.themeTransition='capturing';
      const transition=document.startViewTransition(commit);
      await transition.ready;
      root.dataset.themeTransition='animating';
      await transition.finished;
    }catch{commit();}
    finally{
      root.classList.remove('is-theme-revealing','is-theme-fading');root.style.removeProperty('--theme-reveal-center');root.style.removeProperty('--theme-reveal-radius');
      delete root.dataset.themeTransition;
      button.removeAttribute('aria-busy');switching=false;
    }
  });
}
