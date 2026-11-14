// Run manually on the private host when ready to consent. Never run during builds/tests.
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { oauthRequest, saveRefreshToken, validateTokens, outlookScope } from '../server/outlook-auth.mjs';
export async function connect({ clientId, file }, {
  fetcher = fetch, wait = ms => new Promise(resolve=>setTimeout(resolve,ms)), log = console.log,
  save = saveRefreshToken, now = Date.now
} = {}) {
  if(!/^[a-f0-9-]{36}$/i.test(clientId??'')||!file||!path.isAbsolute(file))throw Error();
  const device=await oauthRequest('devicecode',{client_id:clientId,scope:outlookScope()},fetcher);
  if(typeof device.device_code!=='string'||typeof device.user_code!=='string'||
      !Number.isFinite(device.expires_in)||device.expires_in<=0||!Number.isFinite(device.interval)||device.interval<0||device.interval>60)throw Error();
  // Only the short-lived user code is displayed to the consenting operator, never tokens.
  log('Open https://microsoft.com/devicelogin and enter the code: '+device.user_code);
  log('Use your personal account and confirm the read-only calendar permission.');
  const end=now()+Math.min(device.expires_in,900)*1000;let interval=Math.max(5,device.interval);
  while(now()<end) {
    await wait(interval*1000);
    if(now()>=end)break;
    try {
      const tokens=validateTokens(await oauthRequest('token',{client_id:clientId,
        grant_type:'urn:ietf:params:oauth:grant-type:device_code',device_code:device.device_code},fetcher));
      await save(file,clientId,tokens.refresh_token);
      log('Private refresh token saved. No calendar events were changed.');return;
    } catch(error) {
      if(error.code==='slow_down')interval+=5;
      else if(error.code!=='authorization_pending')throw Error();
    }
  }
  throw Error();
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  connect({clientId:process.env.OUTLOOK_CLIENT_ID,file:process.env.OUTLOOK_TOKEN_FILE})
    .catch(()=>{console.error('Outlook connection failed; check registration and retry when ready.');process.exitCode=1;});
}
