import { site } from '../utils/content';
export const prerender=true;
export function GET(){return new Response(`User-agent: *\nAllow: /\n\nSitemap: ${process.env.SITE_URL||site.url}/sitemap-index.xml\n`,{headers:{'Content-Type':'text/plain; charset=utf-8'}})}
