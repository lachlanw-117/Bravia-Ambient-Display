import { promises as fs } from 'node:fs';
import path from 'node:path';
import { randomUUID } from 'node:crypto';

export const AUTH_BASE = 'https://login.microsoftonline.com/consumers/oauth2/v2.0';
export const outlookScope = (readDescriptions = false) => 'offline_access https://graph.microsoft.com/' + (readDescriptions ? 'Calendars.Read' : 'Calendars.ReadBasic');
export async function oauthRequest(endpoint, values, fetcher = fetch) {
  if(!['devicecode','token'].includes(endpoint))throw Error('Invalid OAuth operation');
  const response = await fetcher(AUTH_BASE + '/' + endpoint, { method: 'POST', redirect: 'error',
    signal: AbortSignal.timeout(15000), headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(values).toString() });
  let size=0;const chunks=[];
  for await(const chunk of response.body) {
    size+=chunk.length;if(size>65536)throw Error('Invalid OAuth response');chunks.push(Buffer.from(chunk));
  }
  const value=JSON.parse(Buffer.concat(chunks).toString('utf8'));
  if(!response.ok) {
    const error=Error('Outlook authorisation failed');
    error.code=['authorization_pending','slow_down'].includes(value.error)?value.error:'failed';throw error;
  }
  return value;
}
export function validateTokens(value) {
  if(typeof value?.access_token!=='string'||!value.access_token||value.access_token.length>16384||
      typeof value.refresh_token!=='string'||!value.refresh_token||value.refresh_token.length>16384||
      !Number.isFinite(value.expires_in)||value.expires_in<60||value.expires_in>86400||
      value.token_type!=='Bearer')throw Error('Invalid Outlook tokens');
  return value;
}
// The directory must be private and outside the source tree. No tokens in logs or HTTP output.
export async function saveRefreshToken(file, clientId, refreshToken) {
  if(!path.isAbsolute(file))throw Error('Use an absolute private token path');
  await fs.mkdir(path.dirname(file),{recursive:true,mode:0o700});
  const temp=file+'.'+randomUUID()+'.tmp';
  try {
    const handle=await fs.open(temp,'wx',0o600);
    try {await handle.writeFile(JSON.stringify({clientId,refreshToken}));await handle.sync();} finally {await handle.close();}
    await fs.rename(temp,file);
  } finally {await fs.unlink(temp).catch(()=>{});}
}
export function outlookTokenProvider({ clientId, tokenFile, readDescriptions = false }, fetcher = fetch) {
  let access=null,until=0,pending=null;
  return async () => {
    if(access&&Date.now()<until)return access;
    if(pending)return pending;
    pending=(async()=>{
      if(!/^[a-f0-9-]{36}$/i.test(clientId)||!path.isAbsolute(tokenFile))throw Error('Configure Outlook access');
      const stat=await fs.lstat(tokenFile);
      if(!stat.isFile()||stat.isSymbolicLink()||stat.size>65536||
          (process.platform!=='win32'&&(stat.mode&0o077)))throw Error('Invalid private token file');
      const saved=JSON.parse(await fs.readFile(tokenFile,'utf8'));
      if(saved.clientId!==clientId||typeof saved.refreshToken!=='string'||!saved.refreshToken||saved.refreshToken.length>16384)throw Error('Invalid private token');
      const next=validateTokens(await oauthRequest('token',{client_id:clientId,grant_type:'refresh_token',
        refresh_token:saved.refreshToken,scope:outlookScope(readDescriptions)},fetcher));
      await saveRefreshToken(tokenFile,clientId,next.refresh_token);
      access=next.access_token;until=Date.now()+(next.expires_in-60)*1000;return access;
    })();
    try{return await pending;}finally{pending=null;}
  };
}
