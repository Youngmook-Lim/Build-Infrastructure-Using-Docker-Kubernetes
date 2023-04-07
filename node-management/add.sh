#!/bin/bash

# 사용자로부터 입력 받기
echo "To add a node, enter your username and IP address (for example, ubuntu@1.2.3.4)."
echo "To finish typing, enter a blank line."

# 빈 줄이 입력될 때까지 IP 주소를 받음
while true; do
    read -p "Username and IP: " input

    # 빈 줄이 입력되면 종료
    if [[ -z "$input" ]]; then
        break
    fi

    # 입력받은 username과 IP를 join-node에 추가
    sed -i "/\[join-node\]/a $input" join-node
done

echo "The inventory file has been updated."

echo "================================================="
echo "To get started adding nodes, we'll ask you for a few inputs."
read -p "Enter noeport of jenkins service: " nodeport
read -p "Enter credential ID of jenkins: " credential
read -p "Enter jenkins url: " url
echo "Enter Jenkins admin id"
read -s id
echo "Enter Jenkins admin password"
read -s password

echo "================================================"
echo "Start the Docker installation."

ansible-playbook -i join-inventory docker-install.yml

echo "================================================"
echo "Start the Kubernetes installation."

ansible-playbook -i join-inventory kubernetes-install.yml

echo "================================================"
echo "Turn off swap and change containerd's runtime to systemd."

ansible-playbook -i join-inventory swapoff-and-containerd-setting.yml

echo "================================================"
echo "joins the kubernetes cluster."

ansible-playbook -i join-inventory join-node.yml


echo "================================================"
echo "Register the agent with the Jenkins master"

join_node_section=false

while read -r line; do
    if [[ $line == "[join-node]" ]]; then
        join_node_section=true
    elif [[ $join_node_section == true && $line == "["* ]]; then
        join_node_section=false
    elif [[ $join_node_section == true ]]; then
        ip_address="${line##*@}"
        jenkins-cli/add-agent.sh -s $url $nodeport "agent-$ip_address" "agent" $credential
    fi
done < "join-node"

awk 'BEGIN {delete_section=0} /^[\[][join-node][\]]/ {delete_section=1; print; next} /^[\[]/ {delete_section=0} delete_section==0 {print}' join-node > join-node.tmp
mv join-node.tmp join-node

echo "done."