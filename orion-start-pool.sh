#!/bin/bash

# Default values
instances=1
base_port=4002
stop=false
base_subnet=1
network_public=false

# Help message
usage() {
    echo "Usage: $0 [-s <start>] [-x <stop>] [-i <instances>] [-n] [-h]"
    echo "  -s    Start the nodes"
    echo "  -x    Stop all running nodes"
    echo "  -i    Number of instances to start (default: 1)"
    echo "  -n    Create a public network (default: false)"
    echo "  -h    Show this help message"
    exit 1
}

# Parse command line arguments
while getopts "sxi:hn" opt; do
    case $opt in
        s)
            start=true
            ;;
        x)
            stop=true
            ;;
        i)
            instances=$OPTARG
            ;;
        n)
            network_public=true
            ;;
        h)
            usage
            ;;
        \?)
            usage
            ;;
    esac
done

if [ -z "$start" ] && [ "$stop" != true ]; then
    echo "Error: -s parameter is required"
    usage
fi


#
#if [ "$start" = true ]; then
#    for i in $(seq 1 $instances); do
#        export NODE_PORT=$((base_port + i)) # Asigna el puerto dinámicamente
#        export SUBNET_IP=$((base_subnet + i))
#        docker-compose -f docker-compose-individual.yml -p "node$i" up # > logs/node_$NODE_PORT.log
#    done
#fi


if [ "$stop" = true ]; then
    echo "Stopping all running nodes..."
    running_nodes=$(docker ps --filter "name=node" -q)
    if [ ! -z "$running_nodes" ]; then
        docker stop $running_nodes
        docker rm $running_nodes
        docker network prune -f
        echo "All nodes stopped successfully and deleted unused networks"
    else
        echo "No running nodes found"
        docker network prune -f
        echo "Deleted unused networks"
    fi

elif [ "$start" = true ]; then


    if [ "$network_public" = true ]; then
        docker network create \
          --driver bridge \
          --subnet="172.20.0.0/24" \
          "public_network" || echo "La red 'public_network' ya existe."
    else


    for i in $(seq 1 $instances); do
        export NODE_PORT=$((base_port + i))
        export SUBNET_IP=$((base_subnet + i))

        # Crear la red dinamica 'lan_node<i>'
        network_name="lan_node${SUBNET_IP}"
        subnet="192.168.${SUBNET_IP}.0/24"
        echo "Creando red '${network_name}' con subnet '${subnet}'..."
        docker network create \
            --driver bridge \
            --subnet="${subnet}" \
            "${network_name}" || echo "La red '${network_name}' ya existe."

#        yq eval '.services.node as $v | del(.services.node) | .services.node'${i}' = $v' docker-compose-individual.yml > docker-compose-individual-GEN.yml
#        yq eval -i '.services.node'${i}'.networks.lan_node as $v | del(.services.networks.lan_node) | .services.node'${i}'.networks.'${network_name}' = $v'  docker-compose-individual-GEN.yml

        yq eval '
          .services.node as $v
          | del(.services.node)
          | .services.node'"${i}"' = $v
          | .services.node'"${i}"'.networks.lan_node as $n
          | del(.services.node'"${i}"'.networks.lan_node)
          | .services.node'"${i}"'.networks.'"${network_name}"' = $n
          | .networks.lan_node as $l
          | del(.networks.lan_node)
          | .networks.'"${network_name}"' = $l
        ' docker-compose-individual.yml > docker-compose-individual-GEN.yml

        # Ejecutar docker-compose y asociar al contenedor a la red dinamica
        echo "Iniciando nodo ${i} en la red '${network_name}' con puerto '${NODE_PORT}'..."
        docker-compose -f docker-compose-individual-GEN.yml -p "node${i}" up -d --build
        rm -f docker-compose-individual-GEN.yml

    done

        #show instances
        docker ps --filter "name=node"
    fi
fi

# yq eval -i '.services.node as $v | del(.services.node) | .services.node1 = $v' docker-compose-individual.yml
