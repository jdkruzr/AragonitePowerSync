#!/usr/bin/env python3
"""Local lab management only: atomically request retirement or permit fresh enrollment."""
import argparse,json,os,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def transition(state,device_id,action):
 registry=json.loads((state/'devices.json').read_text())
 matches=[d for d in registry.values() if d.get('deviceId')==device_id and d.get('model') in ('Palma2_Pro_C','Go103_2Lumi')]
 if len(matches)!=1:raise ValueError('Expected one known disposable native device')
 d=matches[0];path=state/'device-lifecycle.json';rules=json.loads(path.read_text()) if path.exists() else {}
 if not isinstance(rules,dict):raise ValueError('Invalid lifecycle registry')
 prior=rules.get(device_id,{})
 if action=='retire':
  generation=max(d.get('credentialGeneration',0),prior.get('generation',0))+1
  rule={'generation':generation,'state':'retired'}
 else:
  generation=prior.get('generation')
  if prior.get('state')!='retired' or d.get('credentialState')!='retired' or d.get('gatewayRetiredGeneration')!=generation:
   raise ValueError('Retirement must finish before enrollment is allowed')
  rule={'generation':generation,'state':'awaiting_enrollment'}
 rules[device_id]=rule;temp=path.with_suffix('.tmp')
 with open(temp,'w') as stream:
  os.chmod(temp,0o600);json.dump(rules,stream,indent=2);stream.flush();os.fsync(stream.fileno())
 temp.replace(path)
 fd=os.open(state,os.O_RDONLY)
 try:os.fsync(fd)
 finally:os.close(fd)
 return {'deviceId':device_id,**rule,'requested':True,'completionRequiresFacadeReceipt':True}
def main():
 os.umask(0o077);p=argparse.ArgumentParser(description=__doc__);p.add_argument('action',choices=['retire','allow-reenroll']);p.add_argument('device_id');a=p.parse_args()
 if not re.fullmatch(r'device-[0-9a-f]{12}',a.device_id):p.error('Known lab principal required')
 print(json.dumps(transition(ROOT/'artifacts/gateway-lab/shim',a.device_id,a.action)))
if __name__=='__main__':main()
