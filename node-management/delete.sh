#!/bin.bash

# 사용자로부터 입력 받기
echo "To delete a node, enter your username and IP address (for example, ubuntu@1.2.3.4)."
echo "To finish typing, enter a blank line."

# 빈 줄이 입력될 때까지 IP 주소를 받음
while true; do
    read -p "Username and IP: " input

    # 빈 줄이 입력되면 종료
    if [[ -z "$input" ]]; then
        break
    fi

    # 입력받은 username과 IP를 join-node에 추가
    sed -i "/\[delete-node\]/a $input" delete-node
done

echo "The inventory file has been updated."

echo "================================================="
echo "To get started adding nodes, we'll ask you for a few inputs."
read -p "Enter jenkins url: " url
echo "Enter Jenkins admin id"
read -s id
echo "Enter Jenkins admin password"
read -s password

echo "================================================"
echo "Detach node from cluster"

ansible-playbook -i delete-node delete-node.yml

echo "================================================"
echo "Delete the agent from the Jenkins master"

join_node_section=false

while read -r line; do
    if [[ $line == "[delete-node]" ]]; then
        join_node_section=true
    elif [[ $join_node_section == true && $line == "["* ]]; then
        join_node_section=false
    elif [[ $join_node_section == true ]]; then
        ip_address="${line##*@}"
        java -jar jenkins-cli/jenkins-cli.jar -s $url delete-agent "agent-$ip_address"
    fi
done < "delete-node"

awk 'BEGIN {delete_section=0} /^[\[][join-node][\]]/ {delete_section=1; print; next} /^[\[]/ {delete_section=0} delete_section==0 {print}' delete-node > delete-node.tmp
mv delete-node.tmp delete-node

echo "done."