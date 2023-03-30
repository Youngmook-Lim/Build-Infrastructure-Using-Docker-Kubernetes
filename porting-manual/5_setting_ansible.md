## 1. vim 설치

- Linux 용으로 널리 사용되는 텍스트 편집기인 `vim`을 설치합니다.

```jsx
apt-get install vim
```

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/e8b644fd-d157-4203-b58f-053c62cb58be/Untitled.png)

## 2. Inventory 작성

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

## 3. K8s Master, Build Server에 Password 생성

- K8s Master Server에 접속합니다.
- /etc/ssh/sshd_config 파일 수정(PasswordAuthentication yes)

```jsx
sudo vi /etc/ssh/sshd_config
```

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/eb34d010-3d71-456d-b9b7-2b4faffeb534/Untitled.png)

- root 계정 접속

```jsx
sudo su
```

- `ubuntu` 사용자의 비밀번호 변경

```jsx
passwd ubuntu
```

- `ubuntu` 사용자로 돌아와 ssh 재시작

```
# return to user "ubuntu"
exit

# restart ssh
sudo systemctl restart ssh
```

- 모든 빌드 서버에 대하여 위 과정 실행

## 2. SSH key 생성

- ansible-server에서 SSH key 생성

```
ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa <<< y
```

## 3. K8s Master, Build Server에 SSH key 복사

- ansible-server에서 다음과 같이 입력(SSH key를 해당 서버에 복사)

```
# copy the SSH key to the server
ssh-copy-id -i ~/.ssh/id_rsa.pub ubuntu@[k8s master 또는 빌드 서버 IP]
```

- yes 입력 후 해당 빌드 서버에 저장해둔 비밀번호 입력

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/d2ef23a6-71a1-4c33-b83c-96f966b9c148/Untitled.png)

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/d211db52-1c43-483c-81e5-3142c828e3cd/Untitled.png)

- 나머지 서버에도 위 과정 반복
