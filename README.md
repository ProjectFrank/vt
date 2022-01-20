# version-tracker

## Prerequisites

* You will need [Leiningen][] 2.0.0 or above installed.
* Docker
* docker-compose

[leiningen]: https://github.com/technomancy/leiningen

## Running the tests

```
lein test
```

## Running

To start the web server in development:

```
docker-compose up -d
lein run
```

### Signup

Note: ensure that github token has read permissions on public repos.

```
curl -v -X POST -H 'content-type: application/json' -d '{"username":"username","password":"password","github_token":"github_access_token"}' http://localhost:3000/users
```

### Track repo

```
curl -v -u "username:password" -H "content-type: application/json" -X POST -d '{"owner":"microsoft","repo_name":"vscode"}' http://localhost:3000/repos
```

### List tracked repos

```
curl -v -u "username:password" -H "content-type: application/json" http://localhost:3000/repos
```

### Mark repo seen

Note: use repo ID returned from track repo or tracked repo list as `:repo-id` path param.

```
curl -v -u "username:password" -H "content-type: application/json" -X POST http://localhost:3000/repos/:repo-id/mark-seen
```

## License

Copyright © 2021 FIXME
