properties([
    parameters([
        booleanParam( name: "PUBLISH", defaultValue: isMasterBranch() ),
    ]),
    disableConcurrentBuilds(),
])

ECR_REPOSITORY_URL = '601073193384.dkr.ecr.us-west-2.amazonaws.com/primaldarkness/fluffos-v3'
DISCORD_WEBHOOK = 'https://discordapp.com/api/webhooks/632787712612630550/I8FK922Ipk7a9i1zBOe4ZoZtYbJYTdROYk5Zo-zOuSmSOVVmPsxL8yqREjzzhin-8SE-'


withDiscord {
  timeout( time: 10, unit: 'MINUTES', activity: true ) {
    podTemplate(
        annotations: [
            [ key: 'iam.amazonaws.com/role', value: 'jenkins' ]
        ],
        containers: [
            containerTemplate( name: 'aws', image: 'mesosphere/aws-cli', command: 'cat', ttyEnabled: true ),
            containerTemplate( name: 'docker', image: 'docker:dind', command: 'cat', ttyEnabled: true ),
            containerTemplate( name: 'terraform', image: 'alpine/terragrunt', command: 'cat', ttyEnabled: true ),
            containerTemplate( name: 'node', image: 'node:11', command: 'cat', ttyEnabled: true ),
        ],
        volumes: [
            hostPathVolume( mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock' )
        ] ) {

      node( POD_LABEL ) {
        def branchName = BRANCH_NAME.replaceAll( /[\/\\ ]/, '_' )
        def ciTag = "ci-$branchName-$BUILD_NUMBER"

        currentBuild.description = ciTag

        stage( 'Checkout' ) {
          checkout scm
        }

        stage( 'Containerize' ) {
          container( 'docker' ) {
            sh "docker build -t $ECR_REPOSITORY_URL:latest ."
            sh "docker tag $ECR_REPOSITORY_URL:latest $ECR_REPOSITORY_URL:$ciTag"
          }
        }

        if ( params.PUBLISH || isMasterBranch() ) {
          stage( 'Publish' ) {
            container( 'docker' ) {
              def dockerLogin = container( 'aws' ) {
                sh script: "aws ecr get-login --no-include-email --region=us-west-2", returnStdout: true
              }

              sh "echo \"$dockerLogin\" | sh"

              sh "docker push $ECR_REPOSITORY_URL:latest"
              sh "docker push $ECR_REPOSITORY_URL:$ciTag"
            }
          }

          stage( 'Deploy' ) {
            def deployTag = ciTag

            if ( isMasterBranch() ) {
              deployTag = "prod-$BUILD_NUMBER"

              container( 'docker' ) {
                sh "docker tag $ECR_REPOSITORY_URL:latest $ECR_REPOSITORY_URL:$deployTag"
                sh "docker push $ECR_REPOSITORY_URL:$deployTag"
              }
            }
          }
        }
      }
    }
  }
}

def isMasterBranch() {
  BRANCH_NAME == "master"
}

def withDiscord( Closure block ) {
  try {
    block()
    discordSend webhookURL: DISCORD_WEBHOOK, title: JOB_NAME, result: 'SUCCESS', link: BUILD_URL, description: "Deployed tag ${currentBuild.description}"
  } catch (e) {
    discordSend webhookURL: DISCORD_WEBHOOK, title: JOB_NAME, result: 'FAILURE', link: BUILD_URL
    throw e
  }
}
