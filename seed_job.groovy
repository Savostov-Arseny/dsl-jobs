#!groovy

//List of services
List<String> list = new ArrayList<String>();
list.add("config");
list.add("monitoring");
list.add("registry");
list.add("gateway");
list.add("auth-service");
list.add("account-service");
list.add("statistics-service");
list.add("notification-service");
list.add("turbine-stream-service");
list.add("mongodb");

//Create job for each service

for (String item : list) {

  job('Build_' + item)
{
  description('Build ' + item)

   wrappers {
        credentialsBinding {
          usernamePassword {
                usernameVariable('DockerUser')
                passwordVariable('DockerPassword')
                credentialsId('DockerHub')
            }
        }
    }

  scm
  {
    git{
      branch('master')
      remote{
        url('https://github.com/Savostov-Arseny/piggymetrics.git')
      }
    }
  }
  steps{
if (item != 'mongodb') {
  maven {
    mavenInstallation('maven3')
    goals('-B -DskipTests clean package')
      rootPOM('./'+item+'/pom.xml')
  }
}

    shell("docker build -t " + item + " ./" + item)
    shell("echo \$DockerPassword | docker login -u \$DockerUser --password-stdin")
    shell("docker tag " + item + ":latest \$DockerUser/" + item + ":\$BUILD_TAG")
    shell("docker push \$DockerUser/" + item + ":\$BUILD_TAG")
    shell("docker tag \$DockerUser/" + item + ":\$BUILD_TAG \$DockerUser/" + item + ":latest")
    shell("docker push \$DockerUser/" + item + ":latest")
    shell("docker image prune -f")
    shell("docker rmi " + item + ":latest")
    shell("docker rmi \$DockerUser/" + item + ":\$BUILD_TAG")
    shell("docker rmi \$DockerUser/" + item + ":latest")
		}

}
}

//Create Ansible jobs

job('Ansible_custom')
{
description('Execute ansible playbooks')
parameters {
       choiceParam('Playbook', ['set_up_swarm.yml', 'destroy_swarm.yml', 'docker_prune_agents.yml', 'deploy_latest.yml'])
   }
steps{
  shell("docker run -v /home/ubuntu/management-tools/Ansible/conf:/etc/ansible \
-v /home/ubuntu/management-tools/Ansible/Playbooks:/ansible/playbooks \
-v /opt/swarm_key:/opt/private_key ansible \$Playbook ")
}
}
