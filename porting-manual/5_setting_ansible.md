![header](https://capsule-render.vercel.app/api?type=waving&color=auto&height=300&section=header&text=Setting Ansible&fontSize=70)

## Host 파일 생성 및 SSH 연결

### Host 파일 생성

1. vim 설치

- Linux 용으로 널리 사용되는 텍스트 편집기인 `vim`을 설치합니다.

```jsx
apt-get install -y vim
```

2. Inventory 작성

- Hosts 목록 파일 접속에 접속합니다.

```
sudo vi /etc/ansible/hosts
```

- 다음과 같이 내용을 삽입합니다.

```
[all:vars]
ansible_user=(사용자)

[k8s-workers]
(빌드 서버1 IP)
(빌드 서버2 IP)
(빌드 서버3 IP)

[k8s-master]
(k8s 서버 IP)
```

### SSH 연결

1. K8s Master, Build Server에 Password 생성

- K8s Master Server에 접속합니다.
- /etc/ssh/sshd_config 파일 수정(PasswordAuthentication yes)

```jsx
sudo vi /etc/ssh/sshd_config
```

![image](https://user-images.githubusercontent.com/89143804/229355578-70b1a42e-dc58-4a67-a36b-56abb2195479.png)

- root 계정에 접속합니다.

```jsx
sudo su
```

- 사용자의 비밀번호를 변경합니다.(사용자 이름을 ubuntu라 가정합니다.)

```jsx
passwd ubuntu
```

![image](https://user-images.githubusercontent.com/89143804/229355654-7e91d2cd-f584-4a25-895c-06287bb00c31.png)

- `ubuntu` 사용자로 돌아와 ssh를 재시작합니다.

```
# return to user "ubuntu"
exit

# restart ssh
sudo systemctl restart ssh
```

- **모든 빌드 서버에 대하여 위 과정을 실행합니다.**

2. SSH key 생성

- ansible-server에서 SSH key을 생성합니다.

```
ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa <<< y
```

3. K8s Master, Build Server에 SSH key 복사

- ansible-server에서 SSH key를 해당 서버에 복사하기 위해 다음과 같이 입력합니다.

```
# copy the SSH key to the server
ssh-copy-id -i ~/.ssh/id_rsa.pub ubuntu@[k8s master 또는 빌드 서버 IP]
```

- yes 입력 후 해당 빌드 서버에 저장해둔 비밀번호를 입력합니다.

![image](https://user-images.githubusercontent.com/89143804/229356320-4929fa43-4fd3-45a7-b5e7-cdacd6d2a608.png)

![image](https://user-images.githubusercontent.com/89143804/229356337-937ad8e9-a4b5-4f97-ab99-1a8e8099969c.png)

- 나머지 서버에도 위 과정을 반복합니다. 완료하였다면 [다음 단계로 이동](https://lab.ssafy.com/s08-s-project/S08P21S003/-/blob/develop/porting-manual/6_install_docker_to_others.md)해주세요.
