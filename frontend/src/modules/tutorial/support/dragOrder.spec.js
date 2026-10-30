import {expect,it,vi} from 'vitest'
import {beginOrderDrag,moveOrder} from './dragOrder'
it('can drag the first item to the last slot and vice versa',()=>{expect(moveOrder(['1','2','3'],'1','3')).toEqual(['2','3','1']);expect(moveOrder(['1','2','3'],'3','1')).toEqual(['3','1','2'])})
it('ignores foreign and unchanged IDs instead of moving the last row accidentally',()=>{expect(moveOrder(['1','2'],'other','1')).toBeNull();expect(moveOrder(['1','2'],'1','1')).toBeNull()})
it('sets browser drag data and the move effect',()=>{const dataTransfer={setData:vi.fn()};expect(beginOrderDrag({dataTransfer,target:null},5)).toBe('5');expect(dataTransfer.setData).toHaveBeenCalledWith('text/plain','5');expect(dataTransfer.effectAllowed).toBe('move')})
it('does not start sorting while operating a button or a disabled scope',()=>{const preventDefault=vi.fn();const target=document.createElement('button');expect(beginOrderDrag({target,preventDefault},5)).toBe('');expect(preventDefault).toHaveBeenCalled()})
