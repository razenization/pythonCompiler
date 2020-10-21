.386
.model flat,stdcall
option casemap:none

include     D:\masm32\include\windows.inc
include     D:\masm32\include\kernel32.inc
include     D:\masm32\include\masm32.inc
includelib  D:\masm32\lib\kernel32.lib
includelib  D:\masm32\lib\masm32.lib

_NumbToStr   PROTO :DWORD,:DWORD
_main        PROTO

main PROTO
second PROTO

.data
buff        db 11 dup(?)

.code
_start:
	invoke  _main
	invoke  _NumbToStr, ebx, ADDR buff
	invoke  StdOut,eax
	invoke  ExitProcess,0

_main PROC

	call main
	call second

	ret

_main ENDP

main PROC
mov ebp, esp
add esp, 100

push 21	
pop ebx
mov [4+ebp+4], ebx


mov ebx, [4+ebp+4]
push ebx
push 21	

pop ebx
pop eax
cmp eax, ebx
mov eax, 0
sete al
push eax
pop ebx
sub esp, 100
ret
main ENDP

second PROC
mov ebp, esp
add esp, 100

push 24	
push 21	
pop ebx
pop eax
sub eax, ebx
push eax
pop ebx
sub esp, 100
ret
second ENDP


_NumbToStr PROC uses ebx x:DWORD,buffer:DWORD

	mov     ecx,buffer
	mov     eax,x
	mov     ebx,10
	add     ecx,ebx
@@:
	xor     edx,edx
	div     ebx
	add     edx,48
	mov     BYTE PTR [ecx],dl
	dec     ecx
	test    eax,eax
	jnz     @b
	inc     ecx
	mov     eax,ecx
	ret

_NumbToStr ENDP

END _start
